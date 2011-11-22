
package net.sourceforge.filebot.cli;


import static java.lang.String.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.WebServices.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.filebot.hash.VerificationUtilities.*;
import static net.sourceforge.filebot.subtitle.SubtitleUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.MediaBindingBean;
import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.hash.VerificationFileReader;
import net.sourceforge.filebot.hash.VerificationFileWriter;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.ui.Language;
import net.sourceforge.filebot.ui.rename.HistorySpooler;
import net.sourceforge.filebot.ui.rename.MatchSimilarityMetric;
import net.sourceforge.filebot.vfs.ArchiveType;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.VideoHashSubtitleService;


public class CmdlineOperations implements CmdlineInterface {
	
	@Override
	public List<File> rename(Collection<File> files, String query, String expression, String db, String languageName, boolean strict) throws Exception {
		ExpressionFormat format = (expression != null) ? new ExpressionFormat(expression) : null;
		Locale locale = getLanguage(languageName).toLocale();
		
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		if (mediaFiles.isEmpty()) {
			throw new IllegalArgumentException("No media files: " + files);
		}
		
		if (getEpisodeListProvider(db) != null) {
			// tv series mode
			return renameSeries(files, query, format, getEpisodeListProvider(db), locale, strict);
		}
		
		if (getMovieIdentificationService(db) != null) {
			// movie mode
			return renameMovie(files, query, format, getMovieIdentificationService(db), locale, strict);
		}
		
		// auto-determine mode
		int sxe = 0; // SxE
		int cws = 0; // common word sequence
		double max = mediaFiles.size();
		
		SeriesNameMatcher nameMatcher = new SeriesNameMatcher();
		String[] cwsList = (max >= 5) ? nameMatcher.matchAll(mediaFiles.toArray(new File[0])).toArray(new String[0]) : new String[0];
		
		for (File f : mediaFiles) {
			// count SxE matches
			if (nameMatcher.matchBySeasonEpisodePattern(f.getName()) != null) {
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
		
		CLILogger.finest(format(Locale.ROOT, "Filename pattern: [%.02f] SxE, [%.02f] CWS", sxe / max, cws / max));
		if (sxe >= (max * 0.65) || cws >= (max * 0.65)) {
			return renameSeries(files, query, format, getEpisodeListProviders()[0], locale, strict); // use default episode db
		} else {
			return renameMovie(files, query, format, getMovieIdentificationServices()[0], locale, strict); // use default movie db
		}
	}
	

	public List<File> renameSeries(Collection<File> files, String query, ExpressionFormat format, EpisodeListProvider db, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename episodes using [%s]", db.getName()));
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		Collection<String> seriesNames;
		
		// auto-detect series name if not given
		if (query == null) {
			seriesNames = new SeriesNameMatcher().matchAll(mediaFiles.toArray(new File[0]));
			
			if (seriesNames.isEmpty() || (strict && seriesNames.size() > 1)) {
				throw new Exception("Unable to auto-select series name: " + seriesNames);
			}
			
			query = seriesNames.iterator().next();
			CLILogger.config("Auto-detected series name: " + seriesNames);
		} else {
			seriesNames = singleton(query);
		}
		
		// fetch episode data
		Set<Episode> episodes = fetchEpisodeSet(db, seriesNames, locale, strict);
		
		if (episodes.isEmpty()) {
			throw new RuntimeException("Failed to fetch episode data");
		}
		
		// similarity metrics for matching
		SimilarityMetric[] sequence;
		if (strict) {
			sequence = new SimilarityMetric[] { StrictMetric.EpisodeIdentifier, StrictMetric.SubstringFields, StrictMetric.Name }; // use SEI for matching and SN for excluding false positives
		} else {
			sequence = MatchSimilarityMetric.defaultSequence(false); // same as in GUI
		}
		
		List<Match<File, Episode>> matches = new ArrayList<Match<File, Episode>>();
		matches.addAll(match(filter(mediaFiles, VIDEO_FILES), episodes, sequence));
		matches.addAll(match(filter(mediaFiles, SUBTITLE_FILES), episodes, sequence));
		
		if (matches.isEmpty()) {
			throw new RuntimeException("Unable to match files to episode data");
		}
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();
		
		for (Match<File, Episode> match : matches) {
			File file = match.getValue();
			Episode episode = match.getCandidate();
			String newName = (format != null) ? format.format(new MediaBindingBean(episode, file)) : EpisodeFormat.SeasonEpisode.format(episode);
			File newFile = new File(newName + "." + getExtension(file));
			
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
	

	private Set<Episode> fetchEpisodeSet(final EpisodeListProvider db, final Collection<String> names, final Locale locale, final boolean strict) throws Exception {
		List<Callable<List<Episode>>> tasks = new ArrayList<Callable<List<Episode>>>();
		
		// detect series names and create episode list fetch tasks
		for (final String query : names) {
			tasks.add(new Callable<List<Episode>>() {
				
				@Override
				public List<Episode> call() throws Exception {
					List<SearchResult> results = db.search(query, locale);
					
					// select search result
					if (results.size() > 0) {
						SearchResult selectedSearchResult = selectSearchResult(query, results, strict);
						
						if (selectedSearchResult != null) {
							CLILogger.fine(format("Fetching episode data for [%s]", selectedSearchResult.getName()));
							List<Episode> episodes = db.getEpisodeList(selectedSearchResult, locale);
							
							Analytics.trackEvent(db.getName(), "FetchEpisodeList", selectedSearchResult.getName());
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
	

	public List<File> renameMovie(Collection<File> mediaFiles, String query, ExpressionFormat format, MovieIdentificationService db, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename movies using [%s]", db.getName()));
		
		File[] movieFiles = filter(mediaFiles, VIDEO_FILES).toArray(new File[0]);
		File[] subtitleFiles = filter(mediaFiles, SUBTITLE_FILES).toArray(new File[0]);
		Movie[] movieDescriptors;
		
		if (movieFiles.length > 0) {
			// match movie hashes online
			CLILogger.fine(format("Looking up movie by filehash via [%s]", db.getName()));
			movieDescriptors = db.getMovieDescriptors(movieFiles, locale);
		} else {
			// allow subtitles without video files
			movieDescriptors = new Movie[subtitleFiles.length];
			movieFiles = subtitleFiles;
		}
		
		// use user query if search by hash did not return any results, only one query for one movie though
		if (query != null && movieDescriptors.length == 1 && movieDescriptors[0] == null) {
			CLILogger.fine(format("Looking up movie by query [%s]", query));
			movieDescriptors[0] = (Movie) selectSearchResult(query, db.searchMovie(query, locale), strict);
		}
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();
		
		for (int i = 0; i < movieFiles.length; i++) {
			if (movieDescriptors[i] != null) {
				Movie movie = movieDescriptors[i];
				File file = movieFiles[i];
				String newName = (format != null) ? format.format(new MediaBindingBean(movie, file)) : movie.toString();
				File newFile = new File(newName + "." + getExtension(file));
				
				if (isInvalidFilePath(newFile)) {
					CLILogger.config("Stripping invalid characters from new path: " + newName);
					newFile = validateFilePath(newFile);
				}
				
				renameMap.put(file, newFile);
			} else {
				CLILogger.warning("No matching movie: " + movieFiles[i]);
			}
		}
		
		// handle subtitle files
		for (File subtitleFile : subtitleFiles) {
			// check if subtitle corresponds to a movie file (same name, different extension)
			for (int i = 0; i < movieDescriptors.length; i++) {
				if (movieDescriptors != null) {
					String subtitleName = getName(subtitleFile);
					String movieName = getName(movieFiles[i]);
					
					if (subtitleName.equalsIgnoreCase(movieName)) {
						File movieDestination = renameMap.get(movieFiles[i]);
						File subtitleDestination = new File(movieDestination.getParentFile(), getName(movieDestination) + "." + getExtension(subtitleFile));
						renameMap.put(subtitleFile, subtitleDestination);
						
						// movie match found, we're done
						break;
					}
				}
			}
		}
		
		// rename movies
		Analytics.trackEvent("CLI", "Rename", "Movie", renameMap.size());
		return renameAll(renameMap);
	}
	

	@Override
	public List<File> getSubtitles(Collection<File> files, String query, String languageName, String output, String csn) throws Exception {
		Language language = getLanguage(languageName);
		Charset outputEncoding = (csn != null) ? Charset.forName(csn) : null;
		
		// match movie hashes online
		Set<File> remainingVideos = new TreeSet<File>(filter(files, VIDEO_FILES));
		List<File> downloadedSubtitles = new ArrayList<File>();
		
		if (remainingVideos.isEmpty()) {
			throw new IllegalArgumentException("No video files: " + files);
		}
		
		SubtitleFormat outputFormat = null;
		if (output != null) {
			outputFormat = getSubtitleFormatByName(output);
			
			// when rewriting subtitles to target format an encoding must be defined, default to UTF-8
			if (outputEncoding == null) {
				outputEncoding = Charset.forName("UTF-8");
			}
		}
		
		// lookup subtitles by hash
		for (VideoHashSubtitleService service : WebServices.getVideoHashSubtitleServices()) {
			if (remainingVideos.isEmpty()) {
				break;
			}
			
			CLILogger.fine("Looking up subtitles by filehash via " + service.getName());
			
			for (Entry<File, List<SubtitleDescriptor>> it : service.getSubtitleList(remainingVideos.toArray(new File[0]), language.getName()).entrySet()) {
				if (it.getValue() != null && it.getValue().size() > 0) {
					// auto-select first element if there are multiple hash matches for the same video files
					File subtitle = fetchSubtitle(it.getValue().get(0), it.getKey(), outputFormat, outputEncoding);
					Analytics.trackEvent(service.getName(), "DownloadSubtitle", it.getValue().get(0).getLanguageName(), 1);
					
					// download complete, cross this video off the list
					remainingVideos.remove(it.getKey());
					downloadedSubtitles.add(subtitle);
				}
			}
		}
		
		// lookup subtitles by query and filename
		if (query != null && remainingVideos.size() > 0) {
			for (SubtitleProvider service : WebServices.getSubtitleProviders()) {
				if (remainingVideos.isEmpty()) {
					break;
				}
				
				try {
					CLILogger.fine(format("Searching for [%s] at [%s]", query, service.getName()));
					SearchResult searchResult = selectSearchResult(query, service.search(query), false);
					
					CLILogger.config(format("Retrieving subtitles for [%s]", searchResult.getName()));
					List<SubtitleDescriptor> subtitles = service.getSubtitleList(searchResult, language.getName());
					
					for (File video : remainingVideos.toArray(new File[0])) {
						for (SubtitleDescriptor descriptor : subtitles) {
							if (isDerived(descriptor.getName(), video)) {
								File subtitle = fetchSubtitle(descriptor, video, outputFormat, outputEncoding);
								Analytics.trackEvent(service.getName(), "DownloadSubtitle", descriptor.getLanguageName(), 1);
								
								// download complete, cross this video off the list
								remainingVideos.remove(video);
								downloadedSubtitles.add(subtitle);
								break;
							}
						}
					}
				} catch (Exception e) {
					CLILogger.warning(e.getMessage());
				}
			}
		}
		
		// no subtitles for remaining video files
		for (File video : remainingVideos) {
			CLILogger.warning("No matching subtitles found: " + video);
		}
		
		Analytics.trackEvent("CLI", "Download", "Subtitle", downloadedSubtitles.size());
		return downloadedSubtitles;
	}
	

	private File fetchSubtitle(SubtitleDescriptor descriptor, File movieFile, SubtitleFormat outputFormat, Charset outputEncoding) throws Exception {
		// fetch subtitle archive
		CLILogger.info(format("Fetching [%s.%s]", descriptor.getName(), descriptor.getType()));
		ByteBuffer downloadedData = descriptor.fetch();
		
		// extract subtitles from archive
		ArchiveType type = ArchiveType.forName(descriptor.getType());
		MemoryFile subtitleFile;
		
		if (type != ArchiveType.UNDEFINED) {
			// extract subtitle from archive
			subtitleFile = type.fromData(downloadedData).iterator().next();
		} else {
			// assume that the fetched data is the subtitle
			subtitleFile = new MemoryFile(descriptor.getName() + "." + descriptor.getType(), downloadedData);
		}
		
		// subtitle filename is based on movie filename
		String name = getName(movieFile);
		String ext = getExtension(subtitleFile.getName());
		ByteBuffer data = subtitleFile.getData();
		
		if (outputFormat != null || outputEncoding != null) {
			if (outputFormat != null) {
				ext = outputFormat.getFilter().extension(); // adjust extension of the output file
			}
			
			CLILogger.finest(format("Export [%s] as: %s / %s", subtitleFile.getName(), outputFormat, outputEncoding.displayName(Locale.ROOT)));
			data = exportSubtitles(subtitleFile, outputFormat, 0, outputEncoding);
		}
		
		File destination = new File(movieFile.getParentFile(), name + "." + ext);
		CLILogger.config(format("Writing [%s] to [%s]", subtitleFile.getName(), destination.getName()));
		
		writeFile(data, destination);
		return destination;
	}
	

	private List<File> renameAll(Map<File, File> renameMap) throws Exception {
		// rename files
		final List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();
		
		try {
			for (Entry<File, File> it : renameMap.entrySet()) {
				try {
					// rename file, throw exception on failure
					File destination = renameFile(it.getKey(), it.getValue());
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
		for (Entry<File, File> it : renameLog)
			destinationList.add(it.getValue());
		
		return destinationList;
	}
	

	private List<Match<File, Episode>> match(Collection<File> files, Collection<Episode> episodes, SimilarityMetric[] sequence) throws Exception {
		// always use strict fail-fast matcher
		Matcher<File, Episode> matcher = new Matcher<File, Episode>(files, episodes, true, sequence);
		List<Match<File, Episode>> matches = matcher.match();
		
		for (File failedMatch : matcher.remainingValues()) {
			CLILogger.warning("No matching episode: " + failedMatch.getName());
		}
		
		return matches;
	}
	

	private SearchResult selectSearchResult(String query, Iterable<? extends SearchResult> searchResults, boolean strict) throws IllegalArgumentException {
		// auto-select most probable search result
		Map<String, SearchResult> probableMatches = new TreeMap<String, SearchResult>(String.CASE_INSENSITIVE_ORDER);
		
		// use name similarity metric
		SimilarityMetric metric = new NameSimilarityMetric();
		
		// find probable matches using name similarity > 0.9
		for (SearchResult result : searchResults) {
			if (metric.getSimilarity(query, result.getName()) > 0.9) {
				if (!probableMatches.containsKey(result.getName())) {
					probableMatches.put(result.getName(), result);
				}
			}
		}
		
		if (probableMatches.isEmpty() || (strict && probableMatches.size() != 1)) {
			throw new IllegalArgumentException("Failed to auto-select search result: " + probableMatches.values());
		}
		
		// return first and only value
		return probableMatches.values().iterator().next();
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
				throw new IllegalArgumentException("Paths don't share a common root: " + files);
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
			throw new IllegalArgumentException("Illegal output type: " + output);
		}
		
		CLILogger.config("Using output file: " + outputFile);
		compute(root.getPath(), files, outputFile, hashType, csn);
		
		return outputFile;
	}
	

	private boolean check(File verificationFile, File root) throws Exception {
		HashType type = getHashType(verificationFile);
		
		// check if type is supported
		if (type == null)
			throw new IllegalArgumentException("Unsupported format: " + verificationFile);
		
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
	public List<String> fetchEpisodeList(String query, String expression, String db, String languageName) throws Exception {
		// find series on the web and fetch episode list
		ExpressionFormat format = (expression != null) ? new ExpressionFormat(expression) : null;
		EpisodeListProvider service = (db == null) ? TVRage : getEpisodeListProvider(db);
		Locale locale = getLanguage(languageName).toLocale();
		
		SearchResult hit = selectSearchResult(query, service.search(query, locale), false);
		List<String> episodes = new ArrayList<String>();
		
		for (Episode it : service.getEpisodeList(hit, locale)) {
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
	

	private Language getLanguage(String lang) {
		// try to look up by language code
		Language language = Language.getLanguage(lang);
		
		if (language == null) {
			// try too look up by language name
			language = Language.getLanguageByName(lang);
			
			if (language == null) {
				// unable to lookup language
				throw new IllegalArgumentException("Illegal language code: " + lang);
			}
		}
		
		return language;
	}
	
}
