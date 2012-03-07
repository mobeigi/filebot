
package net.sourceforge.filebot.cli;


import static java.lang.String.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.WebServices.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.filebot.hash.VerificationUtilities.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.filebot.subtitle.SubtitleUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.archive.Archive;
import net.sourceforge.filebot.archive.FileMapper;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.MediaBindingBean;
import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.hash.VerificationFileReader;
import net.sourceforge.filebot.hash.VerificationFileWriter;
import net.sourceforge.filebot.media.ReleaseInfo;
import net.sourceforge.filebot.similarity.EpisodeMetrics;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityComparator;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.similarity.StrictEpisodeMetrics;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.ui.Language;
import net.sourceforge.filebot.ui.rename.HistorySpooler;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieFormat;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MoviePart;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.VideoHashSubtitleService;
import net.sourceforge.tuned.FileUtilities.FolderFilter;


public class CmdlineOperations implements CmdlineInterface {
	
	@Override
	public List<File> rename(Collection<File> files, String query, String output, String expression, String db, String sortOrder, String languageName, boolean strict) throws Exception {
		ExpressionFormat format = (expression != null) ? new ExpressionFormat(expression) : null;
		Locale locale = getLanguage(languageName).toLocale();
		File outputDir = (output != null && output.length() > 0) ? new File(output) : null;
		
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		if (mediaFiles.isEmpty()) {
			throw new Exception("No media files: " + files);
		}
		
		if (getEpisodeListProvider(db) != null) {
			// tv series mode
			return renameSeries(files, query, outputDir, format, getEpisodeListProvider(db), SortOrder.forName(sortOrder), locale, strict);
		}
		
		if (getMovieIdentificationService(db) != null) {
			// movie mode
			return renameMovie(files, query, outputDir, format, getMovieIdentificationService(db), locale, strict);
		}
		
		// auto-determine mode
		int sxe = 0; // SxE
		int cws = 0; // common word sequence
		double max = mediaFiles.size();
		
		SeriesNameMatcher nameMatcher = new SeriesNameMatcher(locale);
		Collection<String> cwsList = emptySet();
		if (max >= 5) {
			cwsList = nameMatcher.matchAll(mediaFiles.toArray(new File[0]));
		}
		
		for (File f : mediaFiles) {
			// count SxE matches
			if (nameMatcher.matchByEpisodeIdentifier(f.getName()) != null) {
				sxe++;
			}
			
			// count CWS matches
			for (String base : cwsList) {
				if (base.equalsIgnoreCase(nameMatcher.matchByFirstCommonWordSequence(base, f.getName()))) {
					cws++;
					break;
				}
			}
		}
		
		CLILogger.finest(format("Filename pattern: [%.02f] SxE, [%.02f] CWS", sxe / max, cws / max));
		if (sxe >= (max * 0.65) || cws >= (max * 0.65)) {
			return renameSeries(files, query, outputDir, format, getEpisodeListProviders()[0], SortOrder.forName(sortOrder), locale, strict); // use default episode db
		} else {
			return renameMovie(files, query, outputDir, format, getMovieIdentificationServices()[0], locale, strict); // use default movie db
		}
	}
	
	
	public List<File> renameSeries(Collection<File> files, String query, File outputDir, ExpressionFormat format, EpisodeListProvider db, SortOrder sortOrder, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename episodes using [%s]", db.getName()));
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		
		// similarity metrics for matching
		SimilarityMetric[] sequence = strict ? StrictEpisodeMetrics.defaultSequence(false) : EpisodeMetrics.defaultSequence(false);
		List<Match<File, Episode>> matches = new ArrayList<Match<File, Episode>>();
		
		// auto-determine optimal batch sets
		for (Entry<Set<File>, Set<String>> sameSeriesGroup : mapSeriesNamesByFiles(mediaFiles, locale).entrySet()) {
			List<List<File>> batchSets = new ArrayList<List<File>>();
			
			if (sameSeriesGroup.getValue() != null && sameSeriesGroup.getValue().size() > 0) {
				// handle series name batch set all at once
				batchSets.add(new ArrayList<File>(sameSeriesGroup.getKey()));
			} else {
				// these files don't seem to belong to any series -> handle folder per folder
				batchSets.addAll(mapByFolder(sameSeriesGroup.getKey()).values());
			}
			
			for (List<File> batch : batchSets) {
				// auto-detect series name if not given
				Collection<String> seriesNames = (query == null) ? detectQuery(batch, locale) : singleton(query);
				
				if (strict && seriesNames.size() > 1) {
					throw new Exception("Handling multiple shows requires non-strict matching");
				}
				
				// fetch episode data
				Set<Episode> episodes = fetchEpisodeSet(db, seriesNames, sortOrder, locale, strict);
				
				if (episodes.size() > 0) {
					matches.addAll(matchEpisodes(filter(mediaFiles, VIDEO_FILES), episodes, sequence));
					matches.addAll(matchEpisodes(filter(mediaFiles, SUBTITLE_FILES), episodes, sequence));
				} else {
					CLILogger.warning("Failed to fetch episode data: " + seriesNames);
				}
			}
		}
		
		if (matches.isEmpty()) {
			throw new Exception("Unable to match files to episode data");
		}
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();
		
		for (Match<File, Episode> match : matches) {
			File file = match.getValue();
			Episode episode = match.getCandidate();
			String newName = (format != null) ? format.format(new MediaBindingBean(episode, file)) : validateFileName(EpisodeFormat.SeasonEpisode.format(episode));
			File newFile = new File(outputDir, newName + "." + getExtension(file));
			
			if (isInvalidFilePath(newFile)) {
				CLILogger.config("Stripping invalid characters from new name: " + newName);
				newFile = validateFilePath(newFile);
			}
			
			renameMap.put(file, newFile);
		}
		
		// rename episodes
		Analytics.trackEvent("CLI", "Rename", "Episode", renameMap.size());
		return renameAll(renameMap);
	}
	
	
	private List<Match<File, Episode>> matchEpisodes(Collection<File> files, Collection<Episode> episodes, SimilarityMetric[] sequence) throws Exception {
		// always use strict fail-fast matcher
		Matcher<File, Episode> matcher = new Matcher<File, Episode>(files, episodes, true, sequence);
		List<Match<File, Episode>> matches = matcher.match();
		
		for (File failedMatch : matcher.remainingValues()) {
			CLILogger.warning("No matching episode: " + failedMatch.getName());
		}
		
		return matches;
	}
	
	
	private Set<Episode> fetchEpisodeSet(final EpisodeListProvider db, final Collection<String> names, final SortOrder sortOrder, final Locale locale, final boolean strict) throws Exception {
		List<Callable<List<Episode>>> tasks = new ArrayList<Callable<List<Episode>>>();
		
		// detect series names and create episode list fetch tasks
		for (final String query : names) {
			tasks.add(new Callable<List<Episode>>() {
				
				@Override
				public List<Episode> call() throws Exception {
					List<SearchResult> results = db.search(query, locale);
					
					// select search result
					if (results.size() > 0) {
						List<SearchResult> selectedSearchResults = selectSearchResult(query, results, strict);
						
						if (selectedSearchResults != null) {
							List<Episode> episodes = new ArrayList<Episode>();
							for (SearchResult it : selectedSearchResults) {
								CLILogger.fine(format("Fetching episode data for [%s]", it.getName()));
								episodes.addAll(db.getEpisodeList(it, sortOrder, locale));
								Analytics.trackEvent(db.getName(), "FetchEpisodeList", it.getName());
							}
							
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
				try {
					episodes.addAll(future.get());
				} catch (Exception e) {
					CLILogger.finest(e.getMessage());
				}
			}
			
			// all background workers have finished
			return episodes;
		} finally {
			// destroy background threads
			executor.shutdown();
		}
	}
	
	
	public List<File> renameMovie(Collection<File> files, String query, File outputDir, ExpressionFormat format, MovieIdentificationService service, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename movies using [%s]", service.getName()));
		
		// handle movie files
		List<File> movieFiles = filter(files, VIDEO_FILES);
		List<File> nfoFiles = filter(files, MediaTypes.getDefaultFilter("application/nfo"));
		
		List<File> orphanedFiles = new ArrayList<File>(filter(files, FILES));
		orphanedFiles.removeAll(movieFiles);
		orphanedFiles.removeAll(nfoFiles);
		
		Map<File, List<File>> derivatesByMovieFile = new HashMap<File, List<File>>();
		for (File movieFile : movieFiles) {
			derivatesByMovieFile.put(movieFile, new ArrayList<File>());
		}
		for (File file : orphanedFiles) {
			for (File movieFile : movieFiles) {
				if (isDerived(file, movieFile)) {
					derivatesByMovieFile.get(movieFile).add(file);
					break;
				}
			}
		}
		for (List<File> derivates : derivatesByMovieFile.values()) {
			orphanedFiles.removeAll(derivates);
		}
		
		// match movie hashes online
		final Map<File, Movie> movieByFile = new HashMap<File, Movie>();
		if (query == null) {
			if (movieFiles.size() > 0) {
				try {
					CLILogger.fine(format("Looking up movie by filehash via [%s]", service.getName()));
					Map<File, Movie> hashLookup = service.getMovieDescriptors(movieFiles, locale);
					movieByFile.putAll(hashLookup);
					Analytics.trackEvent(service.getName(), "HashLookup", "Movie", hashLookup.size()); // number of positive hash lookups
				} catch (UnsupportedOperationException e) {
					CLILogger.fine(format("%s: Hash lookup not supported", service.getName()));
				}
			}
			for (File nfo : nfoFiles) {
				try {
					Movie movie = grepMovie(nfo, service, locale);
					movieByFile.put(nfo, movie);
					
					// match movie info to movie files that match the nfo file name
					SortedSet<File> siblingMovieFiles = new TreeSet<File>(filter(movieFiles, new FolderFilter(nfo.getParentFile())));
					String baseName = stripReleaseInfo(getName(nfo));
					
					for (File movieFile : siblingMovieFiles) {
						if (baseName.equalsIgnoreCase(stripReleaseInfo(getName(movieFile)))) {
							movieByFile.put(movieFile, movie);
						}
					}
				} catch (NoSuchElementException e) {
					CLILogger.warning("Failed to grep IMDbID: " + nfo.getName());
				}
			}
		} else {
			CLILogger.fine(format("Looking up movie by query [%s]", query));
			Movie result = (Movie) selectSearchResult(query, service.searchMovie(query, locale), strict).get(0);
			// force all mappings
			for (File file : files) {
				movieByFile.put(file, result);
			}
		}
		
		// collect files that will be matched one by one
		List<File> movieMatchFiles = new ArrayList<File>();
		movieMatchFiles.addAll(movieFiles);
		movieMatchFiles.addAll(nfoFiles);
		movieMatchFiles.addAll(filter(files, new ReleaseInfo().getDiskFolderFilter()));
		movieMatchFiles.addAll(filter(orphanedFiles, SUBTITLE_FILES)); // run movie detection only on orphaned subtitle files
		
		// map movies to (possibly multiple) files (in natural order) 
		Map<Movie, SortedSet<File>> filesByMovie = new HashMap<Movie, SortedSet<File>>();
		
		// map all files by movie
		for (final File file : movieMatchFiles) {
			Movie movie = movieByFile.get(file);
			
			// unknown hash, try via imdb id from nfo file
			if (movie == null) {
				CLILogger.fine(format("Auto-detect movie from context: [%s]", file));
				Collection<Movie> results = detectMovie(file, null, service, locale, strict);
				movie = (Movie) selectSearchResult(query, results, strict).get(0);
				
				if (movie != null) {
					Analytics.trackEvent(service.getName(), "SearchMovie", movie.toString(), 1);
				}
			}
			
			// check if we managed to lookup the movie descriptor
			if (movie != null) {
				// get file list for movie
				SortedSet<File> movieParts = filesByMovie.get(movie);
				
				if (movieParts == null) {
					movieParts = new TreeSet<File>();
					filesByMovie.put(movie, movieParts);
				}
				
				movieParts.add(file);
			}
		}
		
		// collect all File/MoviePart matches
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		for (Entry<Movie, SortedSet<File>> entry : filesByMovie.entrySet()) {
			for (List<File> fileSet : mapByExtension(entry.getValue()).values()) {
				// resolve movie parts
				for (int i = 0; i < fileSet.size(); i++) {
					Movie moviePart = entry.getKey();
					if (fileSet.size() > 1) {
						moviePart = new MoviePart(moviePart, i + 1, fileSet.size());
					}
					
					matches.add(new Match<File, Movie>(fileSet.get(i), moviePart.clone()));
					
					// automatically add matches for derivate files
					List<File> derivates = derivatesByMovieFile.get(fileSet.get(i));
					if (derivates != null) {
						for (File derivate : derivates) {
							matches.add(new Match<File, Movie>(derivate, moviePart.clone()));
						}
					}
				}
			}
		}
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();
		
		for (Match<File, ?> match : matches) {
			File file = match.getValue();
			Object movie = match.getCandidate();
			String newName = (format != null) ? format.format(new MediaBindingBean(movie, file)) : validateFileName(MovieFormat.NameYear.format(movie));
			File newFile = new File(outputDir, newName + "." + getExtension(file));
			
			if (isInvalidFilePath(newFile)) {
				CLILogger.config("Stripping invalid characters from new path: " + newName);
				newFile = validateFilePath(newFile);
			}
			
			renameMap.put(file, newFile);
		}
		
		// rename movies
		Analytics.trackEvent("CLI", "Rename", "Movie", renameMap.size());
		return renameAll(renameMap);
	}
	
	
	public List<File> renameAll(Map<File, File> renameMap) throws Exception {
		// rename files
		final List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();
		
		try {
			for (Entry<File, File> it : renameMap.entrySet()) {
				try {
					// rename file, throw exception on failure
					File destination = moveRename(it.getKey(), it.getValue());
					CLILogger.info(format("Renamed [%s] to [%s]", it.getKey(), it.getValue()));
					
					// remember successfully renamed matches for history entry and possible revert 
					renameLog.add(new SimpleImmutableEntry<File, File>(it.getKey(), destination));
				} catch (IOException e) {
					CLILogger.warning(format("Failed to rename [%s]", it.getKey()));
					throw e;
				}
			}
		} catch (Exception e) {
			// could not rename one of the files, revert all changes
			CLILogger.severe(e.getMessage());
			
			// revert rename operations in reverse order
			for (ListIterator<Entry<File, File>> iterator = renameLog.listIterator(renameLog.size()); iterator.hasPrevious();) {
				Entry<File, File> mapping = iterator.previous();
				
				// revert rename
				if (mapping.getValue().renameTo(mapping.getKey())) {
					// remove reverted rename operation from log
					CLILogger.info("Reverted filename: " + mapping.getKey());
				} else {
					// failed to revert rename operation
					CLILogger.severe("Failed to revert filename: " + mapping.getValue());
				}
			}
			
			throw new Exception("Renaming failed", e);
		} finally {
			if (renameLog.size() > 0) {
				// update rename history
				HistorySpooler.getInstance().append(renameMap.entrySet());
				
				// printer number of renamed files if any
				CLILogger.fine(format("Renamed %d files", renameLog.size()));
			}
		}
		
		// new file names
		List<File> destinationList = new ArrayList<File>();
		for (Entry<File, File> it : renameLog) {
			destinationList.add(it.getValue());
		}
		
		return destinationList;
	}
	
	
	@Override
	public List<File> getSubtitles(Collection<File> files, String query, String languageName, String output, String csn, boolean strict) throws Exception {
		final Language language = getLanguage(languageName);
		
		// when rewriting subtitles to target format an encoding must be defined, default to UTF-8
		final Charset outputEncoding = (csn != null) ? Charset.forName(csn) : (output != null) ? Charset.forName("UTF-8") : null;
		final SubtitleFormat outputFormat = (output != null) ? getSubtitleFormatByName(output) : null;
		
		// try to find subtitles for each video file 
		SubtitleCollector collector = new SubtitleCollector(filter(files, VIDEO_FILES));
		
		if (collector.isComplete()) {
			throw new Exception("No video files: " + files);
		}
		
		// lookup subtitles by hash
		for (VideoHashSubtitleService service : WebServices.getVideoHashSubtitleServices()) {
			if (collector.isComplete()) {
				break;
			}
			
			try {
				CLILogger.fine("Looking up subtitles by filehash via " + service.getName());
				collector.addAll(service.getName(), lookupSubtitleByHash(service, language, collector.remainingVideos()));
			} catch (Exception e) {
				CLILogger.warning(format("Lookup by hash failed: " + e.getMessage()));
			}
		}
		
		// lookup subtitles via text search, only perform hash lookup in strict mode
		if ((query != null || !strict) && !collector.isComplete()) {
			// auto-detect search query
			Collection<String> querySet = (query == null) ? detectQuery(filter(files, VIDEO_FILES), language.toLocale()) : singleton(query);
			
			for (SubtitleProvider service : WebServices.getSubtitleProviders()) {
				if (collector.isComplete()) {
					break;
				}
				
				try {
					CLILogger.fine(format("Searching for %s at [%s]", querySet.toString(), service.getName()));
					collector.addAll(service.getName(), lookupSubtitleByFileName(service, querySet, language, collector.remainingVideos()));
				} catch (Exception e) {
					CLILogger.warning(format("Search for [%s] failed: %s", querySet, e.getMessage()));
				}
			}
		}
		
		// no subtitles for remaining video files
		for (File it : collector.remainingVideos()) {
			CLILogger.warning("No matching subtitles found: " + it);
		}
		
		// download subtitles in order
		Map<File, Callable<File>> downloadQueue = new TreeMap<File, Callable<File>>();
		for (final Entry<String, Map<File, SubtitleDescriptor>> source : collector.subtitlesBySource().entrySet()) {
			for (final Entry<File, SubtitleDescriptor> descriptor : source.getValue().entrySet()) {
				downloadQueue.put(descriptor.getKey(), new Callable<File>() {
					
					@Override
					public File call() throws Exception {
						Analytics.trackEvent(source.getKey(), "DownloadSubtitle", descriptor.getValue().getLanguageName(), 1);
						return downloadSubtitle(descriptor.getValue(), descriptor.getKey(), outputFormat, outputEncoding);
					}
				});
			}
		}
		
		// parallel download
		List<File> subtitleFiles = new ArrayList<File>();
		
		if (downloadQueue.size() > 0) {
			ExecutorService executor = Executors.newFixedThreadPool(4);
			
			try {
				for (Future<File> it : executor.invokeAll(downloadQueue.values())) {
					subtitleFiles.add(it.get());
				}
			} finally {
				executor.shutdownNow();
			}
		}
		
		Analytics.trackEvent("CLI", "Download", "Subtitle", subtitleFiles.size());
		return subtitleFiles;
	}
	
	
	public List<File> getMissingSubtitles(Collection<File> files, String query, String languageName, String output, String csn, boolean strict) throws Exception {
		List<File> videoFiles = filter(filter(files, VIDEO_FILES), new FileFilter() {
			
			// save time on repeating filesystem calls
			private final Map<File, File[]> cache = new HashMap<File, File[]>();
			
			
			@Override
			public boolean accept(File video) {
				File[] subtitlesByFolder = cache.get(video.getParentFile());
				if (subtitlesByFolder == null) {
					subtitlesByFolder = video.getParentFile().listFiles(SUBTITLE_FILES);
					cache.put(video.getParentFile(), subtitlesByFolder);
				}
				
				for (File subtitle : subtitlesByFolder) {
					if (isDerived(subtitle, video))
						return false;
				}
				
				return true;
			}
		});
		
		if (videoFiles.isEmpty()) {
			CLILogger.info("No missing subtitles");
			return emptyList();
		}
		
		CLILogger.finest(format("Missing subtitles for %d video files", videoFiles.size()));
		return getSubtitles(videoFiles, query, languageName, output, csn, strict);
	}
	
	
	private File downloadSubtitle(SubtitleDescriptor descriptor, File movieFile, SubtitleFormat outputFormat, Charset outputEncoding) throws Exception {
		// fetch subtitle archive
		CLILogger.info(format("Fetching [%s]", descriptor.getPath()));
		MemoryFile subtitleFile = fetchSubtitle(descriptor);
		
		// subtitle filename is based on movie filename
		String base = getName(movieFile);
		String ext = getExtension(subtitleFile.getName());
		ByteBuffer data = subtitleFile.getData();
		
		if (outputFormat != null || outputEncoding != null) {
			if (outputFormat != null) {
				ext = outputFormat.getFilter().extension(); // adjust extension of the output file
			}
			
			CLILogger.finest(format("Export [%s] as: %s / %s", subtitleFile.getName(), outputFormat, outputEncoding.displayName(Locale.ROOT)));
			data = exportSubtitles(subtitleFile, outputFormat, 0, outputEncoding);
		}
		
		File destination = new File(movieFile.getParentFile(), formatSubtitle(base, descriptor.getLanguageName(), ext));
		CLILogger.config(format("Writing [%s] to [%s]", subtitleFile.getName(), destination.getName()));
		
		writeFile(data, destination);
		return destination;
	}
	
	
	private Map<File, SubtitleDescriptor> lookupSubtitleByHash(VideoHashSubtitleService service, Language language, Collection<File> videoFiles) throws Exception {
		Map<File, SubtitleDescriptor> subtitleByVideo = new HashMap<File, SubtitleDescriptor>(videoFiles.size());
		
		for (Entry<File, List<SubtitleDescriptor>> it : service.getSubtitleList(videoFiles.toArray(new File[0]), language.getName()).entrySet()) {
			if (it.getValue() != null && it.getValue().size() > 0) {
				CLILogger.finest(format("Matched [%s] to [%s] via filehash", it.getKey().getName(), it.getValue().get(0).getName()));
				subtitleByVideo.put(it.getKey(), it.getValue().get(0));
			}
		}
		
		return subtitleByVideo;
	}
	
	
	private Map<File, SubtitleDescriptor> lookupSubtitleByFileName(SubtitleProvider service, Collection<String> querySet, Language language, Collection<File> videoFiles) throws Exception {
		Map<File, SubtitleDescriptor> subtitleByVideo = new HashMap<File, SubtitleDescriptor>();
		
		// search for subtitles
		List<SubtitleDescriptor> subtitles = findSubtitles(service, querySet, language.getName());
		
		// match subtitle files to video files
		if (subtitles.size() > 0) {
			// first match everything as best as possible, then filter possibly bad matches
			Matcher<File, SubtitleDescriptor> matcher = new Matcher<File, SubtitleDescriptor>(videoFiles, subtitles, false, EpisodeMetrics.defaultSequence(true));
			SimilarityMetric sanity = EpisodeMetrics.verificationMetric();
			
			for (Match<File, SubtitleDescriptor> it : matcher.match()) {
				if (sanity.getSimilarity(it.getValue(), it.getCandidate()) >= 0.9) {
					CLILogger.finest(format("Matched [%s] to [%s] via filename", it.getValue().getName(), it.getCandidate().getName()));
					subtitleByVideo.put(it.getValue(), it.getCandidate());
				}
			}
		}
		
		return subtitleByVideo;
	}
	
	
	private List<String> detectQuery(Collection<File> mediaFiles, Locale locale) throws Exception {
		// detect series name by common word sequence
		List<String> names = detectSeriesNames(mediaFiles, locale);
		
		if (names.isEmpty()) {
			throw new Exception("Failed to auto-detect query");
		}
		
		CLILogger.config("Auto-detected query: " + names);
		return names;
	}
	
	
	public List<SearchResult> findProbableMatches(final String query, Iterable<? extends SearchResult> searchResults, boolean strict) {
		// auto-select most probable search result
		Map<String, SearchResult> probableMatches = new LinkedHashMap<String, SearchResult>();
		
		// use name similarity metric
		final SimilarityMetric metric = new NameSimilarityMetric();
		
		// find probable matches using name similarity > 0.9 (or > 0.8 in non-strict mode)
		for (SearchResult result : searchResults) {
			float f = (query == null) ? 1 : metric.getSimilarity(query, result.getName());
			if (f >= (strict ? 0.9 : 0.8) || (f >= 0.6 && result.getName().toLowerCase().startsWith(query.toLowerCase()))) {
				if (!probableMatches.containsKey(result.toString().toLowerCase())) {
					probableMatches.put(result.toString().toLowerCase(), result);
				}
			}
		}
		
		// sort results by similarity to query
		List<SearchResult> results = new ArrayList<SearchResult>(probableMatches.values());
		if (query != null) {
			sort(results, new SimilarityComparator(query));
		}
		return results;
	}
	
	
	public List<SearchResult> selectSearchResult(String query, Iterable<? extends SearchResult> searchResults, boolean strict) throws Exception {
		List<SearchResult> probableMatches = findProbableMatches(query, searchResults, strict);
		
		if (probableMatches.isEmpty() || (strict && probableMatches.size() != 1)) {
			throw new Exception("Failed to auto-select search result: " + searchResults);
		}
		
		// return first and only value
		return probableMatches;
	}
	
	
	private Language getLanguage(String lang) throws Exception {
		// try to look up by language code
		Language language = Language.getLanguage(lang);
		
		if (language == null) {
			// try too look up by language name
			language = Language.getLanguageByName(lang);
			
			if (language == null) {
				// unable to lookup language
				throw new Exception("Illegal language code: " + lang);
			}
		}
		
		return language;
	}
	
	
	private class SubtitleCollector {
		
		private final Map<String, Map<File, SubtitleDescriptor>> collection = new HashMap<String, Map<File, SubtitleDescriptor>>();
		private final Set<File> remainingVideos = new TreeSet<File>();
		
		
		public SubtitleCollector(Collection<File> videoFiles) {
			remainingVideos.addAll(videoFiles);
		}
		
		
		public void addAll(String source, Map<File, SubtitleDescriptor> subtitles) {
			remainingVideos.removeAll(subtitles.keySet());
			
			Map<File, SubtitleDescriptor> subtitlesBySource = collection.get(source);
			if (subtitlesBySource == null) {
				subtitlesBySource = new TreeMap<File, SubtitleDescriptor>();
				collection.put(source, subtitlesBySource);
			}
			
			subtitlesBySource.putAll(subtitles);
		}
		
		
		public Map<String, Map<File, SubtitleDescriptor>> subtitlesBySource() {
			return collection;
		}
		
		
		public Collection<File> remainingVideos() {
			return remainingVideos;
		}
		
		
		public boolean isComplete() {
			return remainingVideos.size() == 0;
		}
	}
	
	
	@Override
	public boolean check(Collection<File> files) throws Exception {
		// only check existing hashes
		boolean result = true;
		
		for (File it : filter(files, MediaTypes.getDefaultFilter("verification"))) {
			result &= check(it, it.getParentFile());
		}
		
		return result;
	}
	
	
	@Override
	public File compute(Collection<File> files, String output, String csn) throws Exception {
		// check common parent for all given files
		File root = null;
		for (File it : files) {
			if (root == null || root.getPath().startsWith(it.getParent()))
				root = it.getParentFile();
			
			if (!it.getParent().startsWith(root.getPath()))
				throw new Exception("Paths don't share a common root: " + files);
		}
		
		// create verification file
		File outputFile;
		HashType hashType;
		
		if (output != null && getExtension(output) != null) {
			// use given filename
			hashType = getHashTypeByExtension(getExtension(output));
			outputFile = new File(root, output);
		} else {
			// auto-select the filename based on folder and type
			hashType = (output != null) ? getHashTypeByExtension(output) : HashType.SFV;
			outputFile = new File(root, root.getName() + "." + hashType.getFilter().extension());
		}
		
		if (hashType == null) {
			throw new Exception("Illegal output type: " + output);
		}
		
		CLILogger.config("Using output file: " + outputFile);
		compute(root.getPath(), files, outputFile, hashType, csn);
		
		return outputFile;
	}
	
	
	private boolean check(File verificationFile, File root) throws Exception {
		HashType type = getHashType(verificationFile);
		
		// check if type is supported
		if (type == null) {
			throw new Exception("Unsupported format: " + verificationFile);
		}
		
		// add all file names from verification file
		CLILogger.fine(format("Checking [%s]", verificationFile.getName()));
		VerificationFileReader parser = new VerificationFileReader(createTextReader(verificationFile), type.getFormat());
		boolean status = true;
		
		try {
			while (parser.hasNext()) {
				try {
					Entry<File, String> it = parser.next();
					
					File file = new File(root, it.getKey().getPath()).getAbsoluteFile();
					String current = computeHash(new File(root, it.getKey().getPath()), type);
					CLILogger.info(format("%s %s", current, file));
					
					if (current.compareToIgnoreCase(it.getValue()) != 0) {
						throw new IOException(format("Corrupted file found: %s [hash mismatch: %s vs %s]", it.getKey(), current, it.getValue()));
					}
				} catch (IOException e) {
					status = false;
					CLILogger.warning(e.getMessage());
				}
			}
		} finally {
			parser.close();
		}
		
		return status;
	}
	
	
	private void compute(String root, Collection<File> files, File outputFile, HashType hashType, String csn) throws IOException, Exception {
		// compute hashes recursively and write to file
		VerificationFileWriter out = new VerificationFileWriter(outputFile, hashType.getFormat(), csn != null ? csn : "UTF-8");
		
		try {
			CLILogger.fine("Computing hashes");
			for (File it : files) {
				if (it.isHidden() || MediaTypes.getDefaultFilter("verification").accept(it))
					continue;
				
				String relativePath = normalizePathSeparators(it.getPath().replace(root, "")).substring(1);
				String hash = computeHash(it, hashType);
				CLILogger.info(format("%s %s", hash, relativePath));
				
				out.write(relativePath, hash);
			}
		} catch (Exception e) {
			outputFile.deleteOnExit(); // delete only partially written files
			throw e;
		} finally {
			out.close();
		}
	}
	
	
	@Override
	public List<String> fetchEpisodeList(String query, String expression, String db, String sortOrderName, String languageName) throws Exception {
		// find series on the web and fetch episode list
		ExpressionFormat format = (expression != null) ? new ExpressionFormat(expression) : null;
		EpisodeListProvider service = (db == null) ? TVRage : getEpisodeListProvider(db);
		SortOrder sortOrder = SortOrder.forName(sortOrderName);
		Locale locale = getLanguage(languageName).toLocale();
		
		SearchResult hit = selectSearchResult(query, service.search(query, locale), false).get(0);
		List<String> episodes = new ArrayList<String>();
		
		for (Episode it : service.getEpisodeList(hit, sortOrder, locale)) {
			String name = (format != null) ? format.format(new MediaBindingBean(it, null)) : EpisodeFormat.SeasonEpisode.format(it);
			episodes.add(name);
		}
		
		return episodes;
	}
	
	
	@Override
	public String getMediaInfo(File file, String expression) throws Exception {
		ExpressionFormat format = new ExpressionFormat(expression != null ? expression : "{fn} [{resolution} {af} {vc} {ac}]");
		return format.format(new MediaBindingBean(file, file));
	}
	
	
	@Override
	public List<File> extract(Collection<File> files, String output) throws Exception {
		// only keep single-volume archives or first part of multi-volume archives
		List<File> archiveFiles = filter(files, Archive.VOLUME_ONE_FILTER);
		List<File> extractedFiles = new ArrayList<File>();
		
		for (File file : archiveFiles) {
			Archive archive = new Archive(file);
			try {
				File outputFolder = (output != null) ? new File(output).getAbsoluteFile() : new File(file.getParentFile(), getNameWithoutExtension(file.getName()));
				
				CLILogger.info(String.format("Extract archive [%s] to [%s]", file.getName(), outputFolder));
				FileMapper outputMapper = new FileMapper(outputFolder, false);
				
				List<File> entries = archive.listFiles();
				for (File entry : entries) {
					extractedFiles.add(outputMapper.getOutputFile(entry));
				}
				
				CLILogger.finest("Extracting files " + entries);
				archive.extract(outputMapper);
			} finally {
				archive.close();
			}
		}
		
		return extractedFiles;
	}
	
}
