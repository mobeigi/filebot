package net.filebot.ui.rename;

import static java.util.Collections.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.filebot.Analytics;
import net.filebot.Settings;
import net.filebot.similarity.CommonSequenceMatcher;
import net.filebot.similarity.EpisodeMatcher;
import net.filebot.similarity.Match;
import net.filebot.ui.SelectDialog;
import net.filebot.web.Episode;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.SearchResult;
import net.filebot.web.SortOrder;

class EpisodeListMatcher implements AutoCompleteMatcher {

	private EpisodeListProvider provider;

	private boolean useAnimeIndex;
	private boolean useSeriesIndex;

	// only allow one fetch session at a time so later requests can make use of cached results
	private final Object providerLock = new Object();

	public EpisodeListMatcher(EpisodeListProvider provider, boolean useSeriesIndex, boolean useAnimeIndex) {
		this.provider = provider;
		this.useSeriesIndex = useSeriesIndex;
		this.useAnimeIndex = useAnimeIndex;
	}

	protected SearchResult selectSearchResult(final String query, final List<SearchResult> searchResults, Map<String, SearchResult> selectionMemory, final Component parent) throws Exception {
		if (searchResults.size() == 1) {
			return searchResults.get(0);
		}

		// auto-select most probable search result
		List<SearchResult> probableMatches = getProbableMatches(query, searchResults);

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

				// restore original dialog size
				Settings prefs = Settings.forPackage(EpisodeListMatcher.class);
				int w = Integer.parseInt(prefs.get("dialog.select.w", "280"));
				int h = Integer.parseInt(prefs.get("dialog.select.h", "300"));
				selectDialog.setPreferredSize(new Dimension(w, h));
				selectDialog.pack();

				// show dialog
				selectDialog.setLocation(getOffsetLocation(selectDialog.getOwner()));
				selectDialog.setVisible(true);

				// remember dialog size
				prefs.put("dialog.select.w", Integer.toString(selectDialog.getWidth()));
				prefs.put("dialog.select.h", Integer.toString(selectDialog.getHeight()));

				// selected value or null if the dialog was canceled by the user
				return selectDialog.getSelectedValue();
			}
		});

		// allow only one select dialog at a time
		synchronized (this) {
			synchronized (selectionMemory) {
				if (selectionMemory.containsKey(query)) {
					return selectionMemory.get(query);
				}

				SwingUtilities.invokeAndWait(showSelectDialog);

				// cache selected value
				selectionMemory.put(query, showSelectDialog.get());
				return showSelectDialog.get();
			}
		}
	}

	protected Set<Episode> fetchEpisodeSet(Collection<String> seriesNames, final SortOrder sortOrder, final Locale locale, final Map<String, SearchResult> selectionMemory, final Component parent) throws Exception {
		List<Callable<List<Episode>>> tasks = new ArrayList<Callable<List<Episode>>>();

		// detect series names and create episode list fetch tasks
		for (final String query : seriesNames) {
			tasks.add(new Callable<List<Episode>>() {

				@Override
				public List<Episode> call() throws Exception {
					List<SearchResult> results = provider.search(query, locale);

					// select search result
					if (results.size() > 0) {
						SearchResult selectedSearchResult = selectSearchResult(query, results, selectionMemory, parent);

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
			executor.shutdownNow();
		}
	}

	@Override
	public List<Match<File, ?>> match(List<File> files, final boolean strict, final SortOrder sortOrder, final Locale locale, final boolean autodetection, final Component parent) throws Exception {
		if (files.isEmpty()) {
			return justFetchEpisodeList(sortOrder, locale, parent);
		}

		// ignore sample files
		final List<File> fileset = filter(files, not(getClutterFileFilter()));

		// focus on movie and subtitle files
		final List<File> mediaFiles = filter(fileset, VIDEO_FILES, SUBTITLE_FILES);

		// assume that many shows will be matched, do it folder by folder
		List<Callable<List<Match<File, ?>>>> tasks = new ArrayList<Callable<List<Match<File, ?>>>>();

		// remember user decisions and only bother user once
		final Map<String, SearchResult> selectionMemory = new TreeMap<String, SearchResult>(CommonSequenceMatcher.getLenientCollator(Locale.ROOT));
		final Map<String, List<String>> inputMemory = new TreeMap<String, List<String>>(CommonSequenceMatcher.getLenientCollator(Locale.ROOT));

		// detect series names and create episode list fetch tasks
		if (strict) {
			// in strict mode simply process file-by-file (ignoring all files that don't contain clear SxE patterns)
			for (final File file : mediaFiles) {
				if (parseEpisodeNumber(file, false) != null || parseDate(file) != null) {
					tasks.add(new Callable<List<Match<File, ?>>>() {

						@Override
						public List<Match<File, ?>> call() throws Exception {
							return matchEpisodeSet(singletonList(file), detectSeriesNames(singleton(file), useSeriesIndex, useAnimeIndex, locale), sortOrder, strict, locale, autodetection, selectionMemory, inputMemory, parent);
						}
					});
				}
			}
		} else {
			// in non-strict mode use the complicated (more powerful but also more error prone) match-batch-by-batch logic
			for (Entry<Set<File>, Set<String>> sameSeriesGroup : mapSeriesNamesByFiles(mediaFiles, locale, useSeriesIndex, useAnimeIndex).entrySet()) {
				final List<List<File>> batchSets = new ArrayList<List<File>>();
				final Collection<String> queries = sameSeriesGroup.getValue();

				if (queries != null && queries.size() > 0) {
					// handle series name batch set all at once -> only 1 batch set
					batchSets.add(new ArrayList<File>(sameSeriesGroup.getKey()));
				} else {
					// these files don't seem to belong to any series -> handle folder per folder -> multiple batch sets
					batchSets.addAll(mapByFolder(sameSeriesGroup.getKey()).values());
				}

				for (final List<File> batchSet : batchSets) {
					tasks.add(new Callable<List<Match<File, ?>>>() {

						@Override
						public List<Match<File, ?>> call() throws Exception {
							return matchEpisodeSet(batchSet, queries, sortOrder, strict, locale, autodetection, selectionMemory, inputMemory, parent);
						}
					});
				}
			}
		}

		// match folder per folder in parallel
		ExecutorService executor = Executors.newFixedThreadPool(getPreferredThreadPoolSize());

		try {
			// merge all episodes
			List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
			for (Future<List<Match<File, ?>>> future : executor.invokeAll(tasks)) {
				// make sure each episode has unique object data
				for (Match<File, ?> it : future.get()) {
					matches.add(new Match<File, Episode>(it.getValue(), ((Episode) it.getCandidate()).clone()));
				}
			}

			// handle derived files
			List<Match<File, ?>> derivateMatches = new ArrayList<Match<File, ?>>();
			SortedSet<File> derivateFiles = new TreeSet<File>(fileset);
			derivateFiles.removeAll(mediaFiles);

			for (File file : derivateFiles) {
				for (Match<File, ?> match : matches) {
					if (file.getParentFile().equals(match.getValue().getParentFile()) && isDerived(file, match.getValue()) && match.getCandidate() instanceof Episode) {
						derivateMatches.add(new Match<File, Object>(file, ((Episode) match.getCandidate()).clone()));
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
					return fileset.indexOf(o1.getValue()) - fileset.indexOf(o2.getValue());
				}
			});

			// all background workers have finished
			return matches;
		} finally {
			// destroy background threads
			executor.shutdownNow();
		}
	}

	public List<Match<File, ?>> matchEpisodeSet(final List<File> files, Collection<String> queries, SortOrder sortOrder, boolean strict, Locale locale, boolean autodetection, Map<String, SearchResult> selectionMemory, Map<String, List<String>> inputMemory, Component parent) throws Exception {
		Set<Episode> episodes = emptySet();

		// detect series name and fetch episode list
		if (autodetection) {
			if (queries != null && queries.size() > 0) {
				// only allow one fetch session at a time so later requests can make use of cached results
				synchronized (providerLock) {
					episodes = fetchEpisodeSet(queries, sortOrder, locale, selectionMemory, parent);
				}
			}
		}

		// require user input if auto-detection has failed or has been disabled
		if (episodes.isEmpty() && !strict) {
			List<String> detectedSeriesNames = detectSeriesNames(files, useSeriesIndex, useAnimeIndex, locale);
			String parentPathHint = normalizePathSeparators(getRelativePathTail(files.get(0).getParentFile(), 2).getPath());
			String suggestion = detectedSeriesNames.size() > 0 ? join(detectedSeriesNames, "; ") : parentPathHint;

			List<String> input;
			synchronized (inputMemory) {
				input = inputMemory.get(suggestion);
				if (input == null || suggestion == null || suggestion.isEmpty()) {
					input = showMultiValueInputDialog(getQueryInputMessage(files), suggestion, parentPathHint, parent);
					inputMemory.put(suggestion, input);
				}
			}

			if (input != null && input.size() > 0) {
				// only allow one fetch session at a time so later requests can make use of cached results
				synchronized (providerLock) {
					episodes = fetchEpisodeSet(input, sortOrder, locale, new HashMap<String, SearchResult>(), parent);
				}
			}
		}

		// find file/episode matches
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();

		// group by subtitles first and then by files in general
		if (episodes.size() > 0) {
			for (List<File> filesPerType : mapByMediaExtension(files).values()) {
				EpisodeMatcher matcher = new EpisodeMatcher(filesPerType, episodes, strict);
				for (Match<File, Object> it : matcher.match()) {
					// in strict mode sanity check the result and only pass back good matches
					if (!strict || isEpisodeNumberMatch(it.getValue(), (Episode) it.getCandidate())) {
						matches.add(new Match<File, Episode>(it.getValue(), ((Episode) it.getCandidate()).clone()));
					}
				}
			}
		}

		return matches;
	}

	protected String getQueryInputMessage(List<File> files) throws Exception {
		int limit = 20;
		List<File> selection = new ArrayList<File>(files);
		if (selection.size() > limit) {
			sort(selection, new Comparator<File>() {

				@Override
				public int compare(File o1, File o2) {
					return Long.compare(o2.length(), o1.length());
				}
			});
			selection = selection.subList(0, limit);
		}

		StringBuilder html = new StringBuilder(512);
		html.append("<html>");
		html.append("Unable to identify the following files:").append("<br>");

		for (File file : sortByUniquePath(selection)) {
			html.append("<nobr>");
			html.append("• ");

			File path = getStructurePathTail(file);
			if (path == null) {
				path = getRelativePathTail(file, 3);
			}

			new TextColorizer().colorizePath(html, path, true);
			html.append("</nobr>");
			html.append("<br>");
		}

		if (selection.size() < files.size()) {
			html.append("• ").append("…").append("<br>");
		}

		html.append("<br>");
		html.append("Please enter series name:");
		html.append("</html>");
		return html.toString();
	}

	public List<Match<File, ?>> justFetchEpisodeList(final SortOrder sortOrder, final Locale locale, final Component parent) throws Exception {
		// require user input
		List<String> input = showMultiValueInputDialog("Enter series name:", "", "Fetch Episode List", parent);

		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		if (input.size() > 0) {
			synchronized (providerLock) {
				Set<Episode> episodes = fetchEpisodeSet(input, sortOrder, locale, new HashMap<String, SearchResult>(), parent);
				for (Episode it : episodes) {
					matches.add(new Match<File, Episode>(null, it));
				}
			}
		}
		return matches;
	}

}
