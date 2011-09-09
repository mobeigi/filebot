
package net.sourceforge.filebot.cli;


import static java.lang.String.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.EpisodeBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.Language;
import net.sourceforge.filebot.ui.panel.rename.HistorySpooler;
import net.sourceforge.filebot.ui.panel.rename.MatchSimilarityMetric;
import net.sourceforge.filebot.vfs.ArchiveType;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.Episode;
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
			SortedSet<File> files = new TreeSet<File>(args.getFiles(true));
			
			if (args.getSubtitles) {
				List<File> subtitles = getSubtitles(files, args.query, args.getLanguage());
				files.addAll(subtitles);
			}
			
			if (args.renameSeries) {
				renameSeries(files, args.query, args.getEpisodeFormat(), args.getEpisodeListProvider(), args.getLanguage().toLocale());
			}
			
			if (args.renameMovie) {
				renameMovie(files, args.getMovieIdentificationService(), args.getLanguage().toLocale());
			}
			
			CLILogger.fine("Done ヾ(＠⌒ー⌒＠)ノ");
			return 0;
		} catch (Exception e) {
			CLILogger.severe(e.getMessage());
			CLILogger.fine("Failure (°_°)");
			return -1;
		}
	}
	

	public void renameSeries(Collection<File> files, String query, ExpressionFormat format, EpisodeListProvider datasource, Locale locale) throws Exception {
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		
		if (mediaFiles.isEmpty()) {
			throw new IllegalArgumentException("No video or subtitle files: " + files);
		}
		
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
		
		CLILogger.fine(format("Fetching episode data for [%s] from [%s]", query, datasource.getName()));
		
		// find series on the web
		SearchResult hit = selectSearchResult(query, datasource.search(query, locale));
		
		// fetch episode list
		List<Episode> episodes = datasource.getEpisodeList(hit, locale);
		
		List<Match<File, Episode>> matches = new ArrayList<Match<File, Episode>>();
		matches.addAll(match(filter(mediaFiles, VIDEO_FILES), episodes));
		matches.addAll(match(filter(mediaFiles, SUBTITLE_FILES), episodes));
		
		if (matches.isEmpty()) {
			throw new RuntimeException("Unable to match files to episode data");
		}
		
		// map old files to new paths by applying formatting and validating filenames
		Map<File, String> renameMap = new LinkedHashMap<File, String>();
		
		for (Match<File, Episode> match : matches) {
			File file = match.getValue();
			String newName = format.format(new EpisodeBindingBean(match.getCandidate(), file));
			
			if (isInvalidFileName(newName)) {
				CLILogger.config("Stripping invalid characters from new name: " + newName);
				newName = validateFileName(newName);
			}
			
			renameMap.put(file, newName + "." + getExtension(file));
		}
		
		// rename episodes
		renameAll(renameMap);
	}
	

	public void renameMovie(Collection<File> files, MovieIdentificationService datasource, Locale locale) throws Exception {
		File[] movieFiles = filter(files, VIDEO_FILES).toArray(new File[0]);
		
		if (movieFiles.length <= 0) {
			throw new IllegalArgumentException("No video files: " + files);
		}
		
		CLILogger.fine(format("Looking up movie by filehash via [%s]", datasource.getName()));
		
		// match movie hashes online
		MovieDescriptor[] movieByFileHash = datasource.getMovieDescriptors(movieFiles, locale);
		
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
		for (File subtitleFile : filter(files, SUBTITLE_FILES)) {
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
		renameAll(renameMap);
	}
	

	public List<File> getSubtitles(Collection<File> files, String query, Language language) throws Exception {
		// match movie hashes online
		Set<File> videos = new TreeSet<File>(filter(files, VIDEO_FILES));
		List<File> downloadedSubtitles = new ArrayList<File>();
		
		if (videos.isEmpty()) {
			throw new IllegalArgumentException("No video files: " + files);
		}
		
		// lookup subtitles by hash
		for (VideoHashSubtitleService service : WebServices.getVideoHashSubtitleServices()) {
			if (videos.isEmpty())
				break;
			
			CLILogger.fine("Looking up subtitles by filehash via " + service.getName());
			
			for (Entry<File, List<SubtitleDescriptor>> it : service.getSubtitleList(videos.toArray(new File[0]), language.getName()).entrySet()) {
				if (it.getValue() != null && it.getValue().size() > 0) {
					// auto-select first element if there are multiple hash matches for the same video files
					File subtitle = fetchSubtitle(it.getValue().get(0), it.getKey());
					
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
								File subtitle = fetchSubtitle(descriptor, video);
								
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
	

	private File fetchSubtitle(SubtitleDescriptor descriptor, File movieFile) throws Exception {
		// fetch subtitle archive
		CLILogger.info(format("Fetching [%s.%s]", descriptor.getName(), descriptor.getType()));
		ByteBuffer downloadedData = descriptor.fetch();
		
		// extract subtitles from archive
		ArchiveType type = ArchiveType.forName(descriptor.getType());
		MemoryFile subtitleData;
		
		if (type != ArchiveType.UNDEFINED) {
			// extract subtitle from archive
			subtitleData = type.fromData(downloadedData).iterator().next();
		} else {
			// assume that the fetched data is the subtitle
			subtitleData = new MemoryFile(descriptor.getName() + "." + descriptor.getType(), downloadedData);
		}
		
		// subtitle filename is based on movie filename
		String subtitleFileName = getNameWithoutExtension(movieFile.getName()) + "." + getExtension(subtitleData.getName());
		File destination = new File(movieFile.getParentFile(), validateFileName(subtitleFileName));
		
		CLILogger.config(format("Writing [%s] to [%s]", subtitleData.getName(), destination.getName()));
		writeFile(subtitleData.getData(), destination);
		
		return destination;
	}
	

	private void renameAll(Map<File, String> renameMap) {
		// rename files
		List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();
		
		try {
			for (Entry<File, String> it : renameMap.entrySet()) {
				try {
					// rename file, throw exception on failure
					File destination = rename(it.getKey(), it.getValue());
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
		}
		
		if (renameLog.size() > 0) {
			// update rename history
			HistorySpooler.getInstance().append(renameMap.entrySet());
			
			// printer number of renamed files if any
			CLILogger.fine(format("Renamed %d files", renameLog.size()));
		}
	}
	

	private List<Match<File, Episode>> match(List<File> files, List<Episode> episodes) throws Exception {
		// strict SxE metric, don't allow in-between values
		SimilarityMetric metric = new SimilarityMetric() {
			
			@Override
			public float getSimilarity(Object o1, Object o2) {
				return MatchSimilarityMetric.EpisodeIdentifier.getSimilarity(o1, o2) >= 1 ? 1 : 0;
			}
		};
		
		// fail-fast matcher
		Matcher<File, Episode> matcher = new Matcher<File, Episode>(files, episodes, true, new SimilarityMetric[] { metric });
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
	
}
