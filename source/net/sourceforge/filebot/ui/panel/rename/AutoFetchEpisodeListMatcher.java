
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.FileBotUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.FileUtilities;


class AutoFetchEpisodeListMatcher extends SwingWorker<List<Match<File, Episode>>, Void> {
	
	private final List<File> files;
	
	private final EpisodeListProvider provider;
	
	private final List<SimilarityMetric> metrics;
	

	public AutoFetchEpisodeListMatcher(EpisodeListProvider provider, Collection<File> files, Collection<SimilarityMetric> metrics) {
		this.provider = provider;
		this.files = new LinkedList<File>(files);
		this.metrics = new ArrayList<SimilarityMetric>(metrics);
	}
	

	public List<File> remainingFiles() {
		return Collections.unmodifiableList(files);
	}
	

	protected Collection<String> detectSeriesNames(Collection<File> files) {
		// detect series name(s) from files
		Collection<String> names = new SeriesNameMatcher().matchAll(files.toArray(new File[0]));
		
		if (names.isEmpty())
			throw new IllegalArgumentException("Cannot determine series name.");
		
		return names;
	}
	

	protected SearchResult selectSearchResult(final String query, final List<SearchResult> searchResults) throws Exception {
		if (searchResults.size() == 1) {
			return searchResults.iterator().next();
		}
		
		final LinkedList<SearchResult> probableMatches = new LinkedList<SearchResult>();
		
		// use name similarity metric
		SimilarityMetric metric = new NameSimilarityMetric();
		
		// find probable matches using name similarity > 0.9
		for (SearchResult result : searchResults) {
			if (metric.getSimilarity(query, result.getName()) > 0.9) {
				probableMatches.add(result);
			}
		}
		
		if (probableMatches.size() == 1) {
			return probableMatches.getFirst();
		}
		
		// show selection dialog on EDT
		final RunnableFuture<SearchResult> showSelectDialog = new FutureTask<SearchResult>(new Callable<SearchResult>() {
			
			@Override
			public SearchResult call() throws Exception {
				// display only probable matches if any
				List<SearchResult> selection = probableMatches.isEmpty() ? searchResults : probableMatches;
				
				// multiple results have been found, user must select one
				SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(null, selection);
				
				selectDialog.getHeaderLabel().setText(String.format("Shows matching '%s':", query));
				selectDialog.getCancelAction().putValue(Action.NAME, "Ignore");
				
				// show dialog
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
	

	protected List<Episode> fetchEpisodeList(Collection<String> seriesNames) throws Exception {
		List<Callable<Collection<Episode>>> tasks = new ArrayList<Callable<Collection<Episode>>>();
		
		// detect series names and create episode list fetch tasks
		for (final String seriesName : seriesNames) {
			tasks.add(new Callable<Collection<Episode>>() {
				
				@Override
				public Collection<Episode> call() throws Exception {
					List<SearchResult> results = provider.search(seriesName);
					
					// select search result
					if (results.size() > 0) {
						SearchResult selectedSearchResult = selectSearchResult(seriesName, results);
						
						if (selectedSearchResult != null) {
							return provider.getEpisodeList(selectedSearchResult);
						}
					}
					
					return Collections.emptyList();
				}
			});
		}
		
		// fetch episode lists concurrently
		List<Episode> episodes = new ArrayList<Episode>();
		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		
		for (Future<Collection<Episode>> future : executor.invokeAll(tasks)) {
			episodes.addAll(future.get());
		}
		
		// destroy background threads
		executor.shutdown();
		
		return episodes;
	}
	

	@Override
	protected List<Match<File, Episode>> doInBackground() throws Exception {
		
		// focus on movie and subtitle files
		List<File> mediaFiles = FileUtilities.filter(files, VIDEO_FILES, SUBTITLE_FILES);
		
		// detect series name and fetch episode list
		List<Episode> episodes = fetchEpisodeList(detectSeriesNames(mediaFiles));
		
		List<Match<File, Episode>> matches = new ArrayList<Match<File, Episode>>();
		
		// group by subtitles first and then by files in general
		for (List<File> filesPerType : mapByFileType(mediaFiles, VIDEO_FILES, SUBTITLE_FILES).values()) {
			Matcher<File, Episode> matcher = new Matcher<File, Episode>(filesPerType, episodes, metrics);
			matches.addAll(matcher.match());
		}
		
		// restore original order
		Collections.sort(matches, new Comparator<Match<File, Episode>>() {
			
			@Override
			public int compare(Match<File, Episode> o1, Match<File, Episode> o2) {
				return files.indexOf(o1.getValue()) - files.indexOf(o2.getValue());
			}
		});
		
		// update remaining files
		for (Match<File, Episode> match : matches) {
			files.remove(match.getValue());
		}
		
		return matches;
	}
	

	protected Map<FileFilter, List<File>> mapByFileType(Collection<File> files, FileFilter... filters) {
		// initialize map, keep filter order
		Map<FileFilter, List<File>> map = new LinkedHashMap<FileFilter, List<File>>(filters.length);
		
		// initialize value lists
		for (FileFilter filter : filters) {
			map.put(filter, new ArrayList<File>());
		}
		
		for (File file : files) {
			for (FileFilter filter : filters) {
				if (filter.accept(file)) {
					map.get(filter).add(file);
					
					// put each value into one group only
					break;
				}
			}
		}
		
		return map;
	}
}
