
package net.sourceforge.filebot.ui.rename;


import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.filebot.web.EpisodeUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.similarity.EpisodeMetrics;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SortOrder;


class EpisodeListMatcher implements AutoCompleteMatcher {
	
	private final EpisodeListProvider provider;
	
	// only allow one fetch session at a time so later requests can make use of cached results
	private final Object providerLock = new Object();
	
	
	public EpisodeListMatcher(EpisodeListProvider provider) {
		this.provider = provider;
	}
	
	
	protected SearchResult selectSearchResult(final String query, final List<SearchResult> searchResults, final Component parent) throws Exception {
		if (searchResults.size() == 1) {
			return searchResults.get(0);
		}
		
		// auto-select most probable search result
		List<SearchResult> probableMatches = new LinkedList<SearchResult>();
		
		// use name similarity metric
		SimilarityMetric metric = new NameSimilarityMetric();
		
		// find probable matches using name similarity >= 0.9
		for (SearchResult result : searchResults) {
			if (metric.getSimilarity(normalizeName(query), normalizeName(result.getName())) >= 0.9) {
				probableMatches.add(result);
			}
		}
		
		// auto-select first and only probable search result
		if (probableMatches.size() == 1) {
			return probableMatches.get(0);
		}
		
		// show selection dialog on EDT
		final RunnableFuture<SearchResult> showSelectDialog = new FutureTask<SearchResult>(new Callable<SearchResult>() {
			
			@Override
			public SearchResult call() throws Exception {
				// multiple results have been found, user must select one
				SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(parent, searchResults);
				
				selectDialog.getHeaderLabel().setText(String.format("Shows matching '%s':", query));
				selectDialog.getCancelAction().putValue(Action.NAME, "Ignore");
				selectDialog.setMinimumSize(new Dimension(250, 150));
				selectDialog.pack();
				
				// show dialog
				selectDialog.setLocation(getOffsetLocation(selectDialog.getOwner()));
				selectDialog.setVisible(true);
				
				// selected value or null if the dialog was canceled by the user
				return selectDialog.getSelectedValue();
			}
		});
		
		// allow only one select dialog at a time
		synchronized (this) {
			SwingUtilities.invokeAndWait(showSelectDialog);
		}
		
		// selected value or null
		return showSelectDialog.get();
	}
	
	
	private String normalizeName(String value) {
		// remove trailing braces, e.g. Doctor Who (2005) -> doctor who
		return removeTrailingBrackets(value).toLowerCase();
	}
	
	
	protected Set<Episode> fetchEpisodeSet(Collection<String> seriesNames, final SortOrder sortOrder, final Locale locale, final Component parent) throws Exception {
		List<Callable<List<Episode>>> tasks = new ArrayList<Callable<List<Episode>>>();
		
		// detect series names and create episode list fetch tasks
		for (final String query : seriesNames) {
			tasks.add(new Callable<List<Episode>>() {
				
				@Override
				public List<Episode> call() throws Exception {
					List<SearchResult> results = provider.search(query, locale);
					
					// select search result
					if (results.size() > 0) {
						SearchResult selectedSearchResult = selectSearchResult(query, results, parent);
						
						if (selectedSearchResult != null) {
							List<Episode> episodes = provider.getEpisodeList(selectedSearchResult, sortOrder, locale);
							Analytics.trackEvent(provider.getName(), "FetchEpisodeList", selectedSearchResult.getName());
							
							return episodes;
						}
					}
					
					return Collections.emptyList();
				}
			});
		}
		
		// fetch episode lists concurrently
		ExecutorService executor = Executors.newCachedThreadPool();
		
		try {
			// merge all episodes
			Set<Episode> episodes = new LinkedHashSet<Episode>();
			
			for (Future<List<Episode>> future : executor.invokeAll(tasks)) {
				episodes.addAll(future.get());
			}
			
			// all background workers have finished
			return episodes;
		} finally {
			// destroy background threads
			executor.shutdown();
		}
	}
	
	
	@Override
	public List<Match<File, ?>> match(final List<File> files, final SortOrder sortOrder, final Locale locale, final boolean autodetection, final Component parent) throws Exception {
		// focus on movie and subtitle files
		final List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		
		// assume that many shows will be matched, do it folder by folder
		List<Callable<List<Match<File, ?>>>> taskPerFolder = new ArrayList<Callable<List<Match<File, ?>>>>();
		
		// detect series names and create episode list fetch tasks
		for (Entry<Set<File>, Set<String>> sameSeriesGroup : mapSeriesNamesByFiles(mediaFiles, locale).entrySet()) {
			List<List<File>> batchSets = new ArrayList<List<File>>();
			
			if (sameSeriesGroup.getValue() != null && sameSeriesGroup.getValue().size() > 0) {
				// handle series name batch set all at once
				batchSets.add(new ArrayList<File>(sameSeriesGroup.getKey()));
			} else {
				// these files don't seem to belong to any series -> handle folder per folder
				batchSets.addAll(mapByFolder(sameSeriesGroup.getKey()).values());
			}
			
			for (final List<File> batchSet : batchSets) {
				taskPerFolder.add(new Callable<List<Match<File, ?>>>() {
					
					@Override
					public List<Match<File, ?>> call() throws Exception {
						return matchEpisodeSet(batchSet, sortOrder, locale, autodetection, parent);
					}
				});
			}
		}
		
		// match folder per folder in parallel
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		try {
			// merge all episodes
			List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
			for (Future<List<Match<File, ?>>> future : executor.invokeAll(taskPerFolder)) {
				matches.addAll(future.get());
			}
			
			// handle derived files
			List<Match<File, ?>> derivateMatches = new ArrayList<Match<File, ?>>();
			SortedSet<File> derivateFiles = new TreeSet<File>(files);
			derivateFiles.removeAll(mediaFiles);
			
			for (File file : derivateFiles) {
				for (Match<File, ?> match : matches) {
					if (file.getParentFile().equals(match.getValue().getParentFile()) && isDerived(file, match.getValue())) {
						derivateMatches.add(new Match<File, Object>(file, match.getCandidate()));
						break;
					}
				}
			}
			
			// add matches from other files that are linked via filenames
			matches.addAll(derivateMatches);
			
			// restore original order
			Collections.sort(matches, new Comparator<Match<File, ?>>() {
				
				@Override
				public int compare(Match<File, ?> o1, Match<File, ?> o2) {
					return files.indexOf(o1.getValue()) - files.indexOf(o2.getValue());
				}
			});
			
			// all background workers have finished
			return matches;
		} finally {
			// destroy background threads
			executor.shutdownNow();
		}
	}
	
	
	public List<Match<File, ?>> matchEpisodeSet(final List<File> files, SortOrder sortOrder, Locale locale, boolean autodetection, Component parent) throws Exception {
		Set<Episode> episodes = emptySet();
		
		// detect series name and fetch episode list
		if (autodetection) {
			Collection<String> names = detectSeriesNames(files, locale);
			if (names.size() > 0) {
				// only allow one fetch session at a time so later requests can make use of cached results
				synchronized (providerLock) {
					episodes = fetchEpisodeSet(names, sortOrder, locale, parent);
				}
			}
		}
		
		// require user input if auto-detection has failed or has been disabled 
		if (episodes.isEmpty()) {
			String suggestion = new SeriesNameMatcher().matchByEpisodeIdentifier(getName(files.get(0)));
			if (suggestion != null) {
				// clean media info / release group info / etc 
				suggestion = stripReleaseInfo(suggestion);
			} else {
				// use folder name
				suggestion = files.get(0).getParentFile().getName();
			}
			
			List<String> input = emptyList();
			synchronized (this) {
				input = showMultiValueInputDialog("Enter series name:", suggestion, files.get(0).getParentFile().getName(), parent);
			}
			
			if (input.size() > 0) {
				// only allow one fetch session at a time so later requests can make use of cached results
				synchronized (providerLock) {
					episodes = fetchEpisodeSet(input, sortOrder, locale, parent);
				}
			}
		}
		
		// find file/episode matches
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		// group by subtitles first and then by files in general
		for (List<File> filesPerType : mapByExtension(files).values()) {
			Matcher<File, Episode> matcher = new Matcher<File, Episode>(filesPerType, episodes, false, EpisodeMetrics.defaultSequence(false));
			matches.addAll(matcher.match());
		}
		
		return matches;
	}
}
