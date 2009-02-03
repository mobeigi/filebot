
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.FileBotUtilities.MOVIE_FILES;
import static net.sourceforge.filebot.FileBotUtilities.SUBTITLE_FILES;
import static net.sourceforge.filebot.web.Episode.formatEpisodeNumbers;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.FileUtilities;


class AutoEpisodeListMatcher extends SwingWorker<List<Match<File, Episode>>, Void> {
	
	private final List<File> files;
	
	private final EpisodeListClient client;
	
	private final Collection<SimilarityMetric> metrics;
	
	
	public AutoEpisodeListMatcher(EpisodeListClient client, List<File> files, Collection<SimilarityMetric> metrics) {
		this.client = client;
		this.files = new LinkedList<File>(files);
		this.metrics = new ArrayList<SimilarityMetric>(metrics);
	}
	

	public Collection<File> remainingFiles() {
		return Collections.unmodifiableCollection(files);
	}
	

	protected Collection<String> detectSeriesNames(Collection<File> files) {
		// detect series name(s) from files
		return new SeriesNameMatcher().matchAll(files.toArray(new File[files.size()]));
	}
	

	protected List<Episode> fetchEpisodeList(Collection<String> seriesNames) throws Exception {
		List<Callable<Collection<Episode>>> tasks = new ArrayList<Callable<Collection<Episode>>>();
		
		// detect series names and create episode list fetch tasks
		for (final String seriesName : seriesNames) {
			tasks.add(new Callable<Collection<Episode>>() {
				
				@Override
				public Collection<Episode> call() throws Exception {
					Collection<SearchResult> searchResults = client.search(seriesName);
					
					if (searchResults.isEmpty())
						return Collections.emptyList();
					
					return formatEpisodeNumbers(client.getEpisodeList(searchResults.iterator().next()), 2);
				}
			});
		}
		
		if (tasks.isEmpty())
			throw new IllegalArgumentException("Failed to auto-detect series name.");
		
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
		List<File> mediaFiles = FileUtilities.filter(files, MOVIE_FILES, SUBTITLE_FILES);
		
		// detect series name and fetch episode list
		List<Episode> episodes = fetchEpisodeList(detectSeriesNames(mediaFiles));
		
		List<Match<File, Episode>> matches = new ArrayList<Match<File, Episode>>();
		
		// group by subtitles first and then by files in general
		for (List<File> filesPerType : mapByFileType(files, MOVIE_FILES, SUBTITLE_FILES).values()) {
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
		Map<FileFilter, List<File>> map = new HashMap<FileFilter, List<File>>(filters.length);
		
		// initialize value lists
		for (FileFilter filter : filters) {
			map.put(filter, new ArrayList<File>());
		}
		
		for (File file : files) {
			for (FileFilter filter : filters) {
				if (filter.accept(file)) {
					// put each value into one group only
					map.get(filter).add(file);
					break;
				}
			}
		}
		
		return map;
	}
}
