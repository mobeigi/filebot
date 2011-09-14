
package net.sourceforge.filebot.cli;


import static java.lang.String.*;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.EpisodeBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
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
import net.sourceforge.filebot.ui.panel.rename.HistorySpooler;
import net.sourceforge.filebot.ui.panel.rename.MatchSimilarityMetric;
import net.sourceforge.filebot.vfs.ArchiveType;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.MovieDescriptor;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.VideoHashSubtitleService;


public class ArgumentProcessor {
	
	public int process(ArgumentBean args) throws Exception {
		try {
			CLILogger.setLevel(args.getLogLevel());
			Set<File> files = new LinkedHashSet<File>(args.getFiles(true));
			
			if (args.getSubtitles) {
				List<File> subtitles = getSubtitles(files, args.query, args.getLanguage(), args.output, args.getEncoding());
				files.addAll(subtitles);
			}
			
			if (args.rename) {
				rename(files, args.query, args.getEpisodeFormat(), args.db, args.getLanguage().toLocale(), !args.nonStrict);
			}
			
			if (args.check) {
				check(files, args.output, args.getEncoding());
			}
			
			CLILogger.finest("Done ヾ(＠⌒ー⌒＠)ノ");
			return 0;
		} catch (Exception e) {
			CLILogger.severe(e.getMessage());
			CLILogger.finest("Failure (°_°)");
			return -1;
		}
	}
	

	public Set<File> rename(Collection<File> files, String query, ExpressionFormat format, String db, Locale locale, boolean strict) throws Exception {
		List<File> videoFiles = filter(files, VIDEO_FILES);
		
		if (videoFiles.isEmpty()) {
			throw new IllegalArgumentException("No video files: " + files);
		}
		
		if (getEpisodeListProvider(db) != null) {
			// tv series mode
			return renameSeries(files, query, format, getEpisodeListProvider(db), locale, strict);
		}
		
		if (getMovieIdentificationService(db) != null) {
			// movie mode
			return renameMovie(files, getMovieIdentificationService(db), locale);
		}
		
		// auto-determine mode
		int sxe = 0; // SxE
		int cws = 0; // common word sequence
		double max = videoFiles.size();
		
		SeriesNameMatcher matcher = new SeriesNameMatcher();
		String[] cwsList = (max >= 5) ? matcher.matchAll(videoFiles.toArray(new File[0])).toArray(new String[0]) : new String[0];
		
		for (File f : videoFiles) {
			// count SxE matches
			if (matcher.matchBySeasonEpisodePattern(f.getName()) != null) {
				sxe++;
			}
			
			// count CWS matches
			for (String base : cwsList) {
				if (base.equalsIgnoreCase(matcher.matchByFirstCommonWordSequence(base, f.getName()))) {
					cws++;
					break;
				}
			}
		}
		
		CLILogger.finest(format(Locale.ROOT, "Filename pattern: [%.02f] SxE, [%.02f] CWS", sxe / max, cws / max));
		if (sxe >= (max * 0.65) || cws >= (max * 0.65)) {
			return renameSeries(files, query, format, getEpisodeListProviders()[0], locale, strict); // use default episode db
		} else {
			return renameMovie(files, getMovieIdentificationServices()[0], locale); // use default movie db
		}
	}
	

	public Set<File> renameSeries(Collection<File> files, String query, ExpressionFormat format, EpisodeListProvider db, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename episodes using [%s]", db.getName()));
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		
		// auto-detect series name if not given
		if (query == null) {
			Collection<String> possibleNames = new SeriesNameMatcher().matchAll(mediaFiles.toArray(new File[0]));
			
			if (possibleNames.size() == 1) {
				query = possibleNames.iterator().next();
				CLILogger.config("Auto-detected series name: " + possibleNames);
			} else {
				throw new Exception("Failed to auto-detect series name: " + possibleNames);
			}
		}
		
		CLILogger.fine(format("Fetching episode data for [%s]", query));
		
		// find series on the web
		SearchResult hit = selectSearchResult(query, db.search(query, locale));
		
		// fetch episode list
		List<Episode> episodes = db.getEpisodeList(hit, locale);
		
		List<Match<File, Episode>> matches = new ArrayList<Match<File, Episode>>();
		matches.addAll(match(filter(mediaFiles, VIDEO_FILES), episodes, strict));
		matches.addAll(match(filter(mediaFiles, SUBTITLE_FILES), episodes, strict));
		
		if (matches.isEmpty()) {
			throw new RuntimeException("Unable to match files to episode data");
		}
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, String> renameMap = new LinkedHashMap<File, String>();
		
		for (Match<File, Episode> match : matches) {
			File file = match.getValue();
			Episode episode = match.getCandidate();
			String newName = (format != null) ? format.format(new EpisodeBindingBean(episode, file)) : EpisodeFormat.SeasonEpisode.format(episode);
			
			if (isInvalidFileName(newName)) {
				CLILogger.config("Stripping invalid characters from new name: " + newName);
				newName = validateFileName(newName);
			}
			
			renameMap.put(file, newName + "." + getExtension(file));
		}
		
		// rename episodes
		return renameAll(renameMap);
	}
	

	public Set<File> renameMovie(Collection<File> mediaFiles, MovieIdentificationService db, Locale locale) throws Exception {
		CLILogger.config(format("Rename movies using [%s]", db.getName()));
		
		File[] movieFiles = filter(mediaFiles, VIDEO_FILES).toArray(new File[0]);
		CLILogger.fine(format("Looking up movie by filehash via [%s]", db.getName()));
		
		// match movie hashes online
		MovieDescriptor[] movieByFileHash = db.getMovieDescriptors(movieFiles, locale);
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, String> renameMap = new LinkedHashMap<File, String>();
		
		for (int i = 0; i < movieFiles.length; i++) {
			if (movieByFileHash[i] != null) {
				String newName = movieByFileHash[i].toString();
				
				if (isInvalidFileName(newName)) {
					CLILogger.config("Stripping invalid characters from new path: " + newName);
					newName = validateFileName(newName);
				}
				
				renameMap.put(movieFiles[i], newName + "." + getExtension(movieFiles[i]));
			} else {
				CLILogger.warning("No matching movie: " + movieFiles[i]);
			}
		}
		
		// handle subtitle files
		for (File subtitleFile : filter(mediaFiles, SUBTITLE_FILES)) {
			// check if subtitle corresponds to a movie file (same name, different extension)
			for (int i = 0; i < movieByFileHash.length; i++) {
				if (movieByFileHash != null) {
					String subtitleName = getName(subtitleFile);
					String movieName = getName(movieFiles[i]);
					
					if (subtitleName.equalsIgnoreCase(movieName)) {
						String movieDestinationName = renameMap.get(movieFiles[i]);
						renameMap.put(subtitleFile, getNameWithoutExtension(movieDestinationName) + "." + getExtension(subtitleFile));
						
						// movie match found, we're done
						break;
					}
				}
			}
		}
		
		// rename episodes
		return renameAll(renameMap);
	}
	

	public List<File> getSubtitles(Collection<File> files, String query, Language language, String output, Charset outputEncoding) throws Exception {
		// match movie hashes online
		Set<File> videos = new TreeSet<File>(filter(files, VIDEO_FILES));
		List<File> downloadedSubtitles = new ArrayList<File>();
		
		if (videos.isEmpty()) {
			throw new IllegalArgumentException("No video files: " + files);
		}
		
		SubtitleFormat outputFormat = null;
		if (output != null) {
			outputFormat = getSubtitleFormatByName(output);
			
			// when rewriting subtitles to target format an encoding must be defined, default to UTF-8
			if (outputEncoding == null)
				outputEncoding = Charset.forName("UTF-8");
			
			CLILogger.config(format("Export as: %s (%s)", outputFormat, outputEncoding.displayName(Locale.ROOT)));
		}
		
		// lookup subtitles by hash
		for (VideoHashSubtitleService service : WebServices.getVideoHashSubtitleServices()) {
			if (videos.isEmpty())
				break;
			
			CLILogger.fine("Looking up subtitles by filehash via " + service.getName());
			
			for (Entry<File, List<SubtitleDescriptor>> it : service.getSubtitleList(videos.toArray(new File[0]), language.getName()).entrySet()) {
				if (it.getValue() != null && it.getValue().size() > 0) {
					// auto-select first element if there are multiple hash matches for the same video files
					File subtitle = fetchSubtitle(it.getValue().get(0), it.getKey(), outputFormat, outputEncoding);
					
					// download complete, cross this video off the list
					videos.remove(it.getKey());
					downloadedSubtitles.add(subtitle);
				}
			}
		}
		
		// lookup subtitles by query and filename
		if (query != null && videos.size() > 0) {
			for (SubtitleProvider service : WebServices.getSubtitleProviders()) {
				try {
					CLILogger.fine(format("Searching for [%s] at [%s]", query, service.getName()));
					SearchResult searchResult = selectSearchResult(query, service.search(query));
					
					CLILogger.config("Retrieving subtitles for " + searchResult.getName());
					List<SubtitleDescriptor> subtitles = service.getSubtitleList(searchResult, language.getName());
					
					for (File video : videos.toArray(new File[0])) {
						String filename = getName(video); // get name without extension
						
						for (SubtitleDescriptor descriptor : subtitles) {
							if (filename.equalsIgnoreCase(descriptor.getName())) {
								File subtitle = fetchSubtitle(descriptor, video, outputFormat, outputEncoding);
								
								// download complete, cross this video off the list
								videos.remove(video);
								downloadedSubtitles.add(subtitle);
							}
						}
					}
				} catch (Exception e) {
					CLILogger.warning(e.getMessage());
				}
			}
		}
		
		// no subtitles for remaining video files
		for (File video : videos) {
			CLILogger.warning("No matching subtitles found: " + video);
		}
		
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
			ext = outputFormat.getFilter().extension(); // adjust extension of the output file
			data = exportSubtitles(subtitleFile, outputFormat, 0, outputEncoding);
		}
		
		File destination = new File(movieFile.getParentFile(), name + "." + ext);
		CLILogger.config(format("Writing [%s] to [%s]", subtitleFile.getName(), destination.getName()));
		
		writeFile(data, destination);
		return destination;
	}
	

	private Set<File> renameAll(Map<File, String> renameMap) throws Exception {
		// rename files
		final List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();
		
		try {
			for (Entry<File, String> it : renameMap.entrySet()) {
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
		Set<File> newFiles = new LinkedHashSet<File>();
		for (Entry<File, File> it : renameLog)
			newFiles.add(it.getValue());
		
		return newFiles;
	}
	

	private List<Match<File, Episode>> match(List<File> files, List<Episode> episodes, boolean strict) throws Exception {
		SimilarityMetric[] sequence = MatchSimilarityMetric.defaultSequence();
		
		if (strict) {
			// strict SxE metric, don't allow in-between values
			SimilarityMetric strictEpisodeMetric = new SimilarityMetric() {
				
				@Override
				public float getSimilarity(Object o1, Object o2) {
					return MatchSimilarityMetric.EpisodeIdentifier.getSimilarity(o1, o2) >= 1 ? 1 : 0;
				}
			};
			
			// use only strict SxE metric
			sequence = new SimilarityMetric[] { strictEpisodeMetric };
		}
		
		// always use strict fail-fast matcher
		Matcher<File, Episode> matcher = new Matcher<File, Episode>(files, episodes, true, sequence);
		List<Match<File, Episode>> matches = matcher.match();
		
		for (File failedMatch : matcher.remainingValues()) {
			CLILogger.warning("No matching episode: " + failedMatch.getName());
		}
		
		return matches;
	}
	

	private SearchResult selectSearchResult(String query, Iterable<SearchResult> searchResults) throws IllegalArgumentException {
		// auto-select most probable search result
		List<SearchResult> probableMatches = new ArrayList<SearchResult>();
		
		// use name similarity metric
		SimilarityMetric metric = new NameSimilarityMetric();
		
		// find probable matches using name similarity > 0.9
		for (SearchResult result : searchResults) {
			if (metric.getSimilarity(query, result.getName()) > 0.9) {
				probableMatches.add(result);
			}
		}
		
		if (probableMatches.size() != 1) {
			throw new IllegalArgumentException("Failed to auto-select search result: " + probableMatches);
		}
		
		return probableMatches.get(0);
	}
	

	public void check(Collection<File> files, String output, Charset outputEncoding) throws Exception {
		// check verification file
		if (containsOnly(files, MediaTypes.getDefaultFilter("verification"))) {
			// only check existing hashes
			boolean ok = true;
			
			for (File it : files) {
				ok &= check(it, it.getParentFile());
			}
			
			if (!ok) {
				throw new Exception("Data corruption detected"); // one or more hashes mismatch
			}
			
			// all hashes match
			return;
		}
		
		// check common parent for all given files
		File root = null;
		for (File it : files) {
			if (root == null || root.getPath().startsWith(it.getParent()))
				root = it.getParentFile();
			
			if (!it.getParent().startsWith(root.getPath()))
				throw new IllegalArgumentException("Path don't share a common root: " + files);
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
		compute(root.getPath(), files, outputFile, hashType, outputEncoding);
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
	

	private void compute(String root, Collection<File> files, File outputFile, HashType hashType, Charset outputEncoding) throws IOException, Exception {
		// compute hashes recursively and write to file
		VerificationFileWriter out = new VerificationFileWriter(outputFile, hashType.getFormat(), outputEncoding != null ? outputEncoding.name() : "UTF-8");
		
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
}
