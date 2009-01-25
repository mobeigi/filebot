
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.FileBotUtilities.SUBTITLE_FILES;
import static net.sourceforge.filebot.FileBotUtilities.asStringList;
import static net.sourceforge.filebot.web.Episode.formatEpisodeNumbers;
import static net.sourceforge.tuned.FileUtilities.FILES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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


class AutoEpisodeListMatcher extends SwingWorker<List<Match<FileEntry, Episode>>, Void> {
	
	private final List<FileEntry> remainingFiles = new ArrayList<FileEntry>();
	
	private final List<FileEntry> files;
	
	private final EpisodeListClient client;
	
	private final Collection<SimilarityMetric> metrics;
	
	
	public AutoEpisodeListMatcher(EpisodeListClient client, List<FileEntry> files, Collection<SimilarityMetric> metrics) {
		this.client = client;
		this.files = files;
		this.metrics = metrics;
	}
	

	public Collection<FileEntry> remainingFiles() {
		return Collections.unmodifiableCollection(remainingFiles);
	}
	

	protected Collection<String> matchSeriesNames(List<FileEntry> episodes) {
		int threshold = Math.min(episodes.size(), 5);
		
		return new SeriesNameMatcher(threshold).matchAll(asStringList(episodes));
	}
	

	@Override
	protected List<Match<FileEntry, Episode>> doInBackground() throws Exception {
		List<Callable<Collection<Episode>>> fetchTasks = new ArrayList<Callable<Collection<Episode>>>();
		
		// match series names and create episode list fetch tasks
		for (final String seriesName : matchSeriesNames(files)) {
			fetchTasks.add(new Callable<Collection<Episode>>() {
				
				@Override
				public Collection<Episode> call() throws Exception {
					Collection<SearchResult> searchResults = client.search(seriesName);
					
					if (searchResults.isEmpty())
						return Collections.emptyList();
					
					return formatEpisodeNumbers(client.getEpisodeList(searchResults.iterator().next()), 2);
				}
			});
		}
		
		if (fetchTasks.isEmpty()) {
			throw new IllegalArgumentException("Failed to auto-detect series name.");
		}
		
		// fetch episode lists concurrently
		List<Episode> episodeList = new ArrayList<Episode>();
		ExecutorService executor = Executors.newFixedThreadPool(fetchTasks.size());
		
		for (Future<Collection<Episode>> future : executor.invokeAll(fetchTasks)) {
			episodeList.addAll(future.get());
		}
		
		executor.shutdown();
		
		List<Match<FileEntry, Episode>> matches = new ArrayList<Match<FileEntry, Episode>>();
		
		for (List<FileEntry> entryList : splitByFileType(files)) {
			Matcher<FileEntry, Episode> matcher = new Matcher<FileEntry, Episode>(entryList, episodeList, metrics);
			matches.addAll(matcher.match());
			remainingFiles.addAll(matcher.remainingValues());
		}
		
		return matches;
	}
	

	@SuppressWarnings("unchecked")
	protected Collection<List<FileEntry>> splitByFileType(Collection<FileEntry> files) {
		List<FileEntry> subtitles = new ArrayList<FileEntry>();
		List<FileEntry> other = new ArrayList<FileEntry>();
		
		for (FileEntry file : files) {
			// check for for subtitles first, then files in general
			if (SUBTITLE_FILES.accept(file.getFile())) {
				subtitles.add(file);
			} else if (FILES.accept(file.getFile())) {
				other.add(file);
			}
		}
		
		return Arrays.asList(other, subtitles);
	}
	
}
