package net.sourceforge.filebot.cli;

import static java.lang.String.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.Settings.*;
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
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Pattern;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.HistorySpooler;
import net.sourceforge.filebot.Language;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.RenameAction;
import net.sourceforge.filebot.archive.Archive;
import net.sourceforge.filebot.archive.FileMapper;
import net.sourceforge.filebot.format.ExpressionFilter;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.MediaBindingBean;
import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.hash.VerificationFileReader;
import net.sourceforge.filebot.hash.VerificationFileWriter;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.similarity.CommonSequenceMatcher;
import net.sourceforge.filebot.similarity.EpisodeMatcher;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityComparator;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.subtitle.SubtitleNaming;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.vfs.SimpleFileInfo;
import net.sourceforge.filebot.web.AudioTrack;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieFormat;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MoviePart;
import net.sourceforge.filebot.web.MusicIdentificationService;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.VideoHashSubtitleService;
import net.sourceforge.tuned.FileUtilities.ParentFilter;

public class CmdlineOperations implements CmdlineInterface {

	@Override
	public List<File> rename(Collection<File> files, RenameAction action, String conflict, String output, String formatExpression, String db, String query, String sortOrder, String filterExpression, String lang, boolean strict) throws Exception {
		ExpressionFormat format = (formatExpression != null) ? new ExpressionFormat(formatExpression) : null;
		ExpressionFilter filter = (filterExpression != null) ? new ExpressionFilter(filterExpression) : null;
		File outputDir = (output != null && output.length() > 0) ? new File(output).getAbsoluteFile() : null;
		Locale locale = getLanguage(lang).getLocale();
		ConflictAction conflictAction = ConflictAction.forName(conflict);

		if (getEpisodeListProvider(db) != null) {
			// tv series mode
			return renameSeries(files, action, conflictAction, outputDir, format, getEpisodeListProvider(db), query, SortOrder.forName(sortOrder), filter, locale, strict);
		}

		if (getMovieIdentificationService(db) != null) {
			// movie mode
			return renameMovie(files, action, conflictAction, outputDir, format, getMovieIdentificationService(db), query, filter, locale, strict);
		}

		if (getMusicIdentificationService(db) != null || containsOnly(files, AUDIO_FILES)) {
			// music mode
			return renameMusic(files, action, conflictAction, outputDir, format, getMusicIdentificationService(db) == null ? AcoustID : getMusicIdentificationService(db));
		}

		// auto-determine mode
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		double max = mediaFiles.size();
		int sxe = 0; // SxE
		int cws = 0; // common word sequence

		SeriesNameMatcher nameMatcher = new SeriesNameMatcher(locale, true);
		Collection<String> cwsList = emptySet();
		if (max >= 5) {
			cwsList = nameMatcher.matchAll(mediaFiles.toArray(new File[0]));
		}

		for (File f : mediaFiles) {
			// count SxE matches
			if (MediaDetection.getEpisodeIdentifier(f.getName(), true) != null) {
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
		if (sxe > (max * 0.65) || cws > (max * 0.65)) {
			return renameSeries(files, action, conflictAction, outputDir, format, TheTVDB, query, SortOrder.forName(sortOrder), filter, locale, strict); // use default episode db
		} else {
			return renameMovie(files, action, conflictAction, outputDir, format, TMDb, query, filter, locale, strict); // use default movie db
		}
	}

	public List<File> renameSeries(Collection<File> files, RenameAction renameAction, ConflictAction conflictAction, File outputDir, ExpressionFormat format, EpisodeListProvider db, String query, SortOrder sortOrder, ExpressionFilter filter, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename episodes using [%s]", db.getName()));

		// ignore sample files
		List<File> fileset = filter(files, not(getClutterFileFilter()));

		List<File> mediaFiles = filter(fileset, VIDEO_FILES, SUBTITLE_FILES);
		if (mediaFiles.isEmpty()) {
			throw new Exception("No media files: " + files);
		}

		// similarity metrics for matching
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();

		// auto-determine optimal batch sets
		for (Entry<Set<File>, Set<String>> sameSeriesGroup : mapSeriesNamesByFiles(mediaFiles, locale, db != AniDB, db == AniDB).entrySet()) {
			List<List<File>> batchSets = new ArrayList<List<File>>();

			if (sameSeriesGroup.getValue() != null && sameSeriesGroup.getValue().size() > 0) {
				// handle series name batch set all at once
				batchSets.add(new ArrayList<File>(sameSeriesGroup.getKey()));
			} else {
				// these files don't seem to belong to any series -> handle folder per folder
				batchSets.addAll(mapByFolder(sameSeriesGroup.getKey()).values());
			}

			for (List<File> batch : batchSets) {
				Collection<String> seriesNames;

				// auto-detect series name if not given
				if (query == null) {
					// detect series name by common word sequence
					seriesNames = detectSeriesNames(batch, db != AniDB, db == AniDB, locale);
					CLILogger.config("Auto-detected query: " + seriesNames);
				} else {
					// use --q option
					seriesNames = asList(query.split("[|]"));
				}

				if (strict && seriesNames.size() > 1) {
					throw new Exception("Handling multiple shows requires non-strict matching");
				}

				if (seriesNames.size() == 0) {
					CLILogger.warning("Failed to detect query for files: " + batch);
					continue;
				}

				// fetch episode data
				Collection<Episode> episodes = fetchEpisodeSet(db, seriesNames, sortOrder, locale, strict);
				if (episodes.size() == 0) {
					CLILogger.warning("Failed to fetch episode data: " + seriesNames);
					continue;
				}

				// filter episodes
				episodes = applyExpressionFilter(episodes, filter);

				matches.addAll(matchEpisodes(filter(batch, VIDEO_FILES), episodes, strict));
				matches.addAll(matchEpisodes(filter(batch, SUBTITLE_FILES), episodes, strict));
			}
		}

		if (matches.isEmpty()) {
			throw new Exception("Unable to match files to episode data");
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

		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();

		for (Match<File, ?> match : matches) {
			File file = match.getValue();
			Object episode = match.getCandidate();
			String newName = (format != null) ? format.format(new MediaBindingBean(episode, file, getContext(matches))) : validateFileName(EpisodeFormat.SeasonEpisode.format(episode));

			renameMap.put(file, getDestinationFile(file, newName, outputDir));
		}

		// rename episodes
		Analytics.trackEvent("CLI", "Rename", "Episode", renameMap.size());
		return renameAll(renameMap, renameAction, conflictAction, matches);
	}

	private List<Match<File, Object>> matchEpisodes(Collection<File> files, Collection<Episode> episodes, boolean strict) throws Exception {
		// always use strict fail-fast matcher
		EpisodeMatcher matcher = new EpisodeMatcher(files, episodes, strict);
		List<Match<File, Object>> matches = matcher.match();

		for (File failedMatch : matcher.remainingValues()) {
			CLILogger.warning("No matching episode: " + failedMatch.getName());
		}

		return matches;
	}

	private Set<Episode> fetchEpisodeSet(final EpisodeListProvider db, final Collection<String> names, final SortOrder sortOrder, final Locale locale, final boolean strict) throws Exception {
		Set<SearchResult> shows = new LinkedHashSet<SearchResult>();
		Set<Episode> episodes = new LinkedHashSet<Episode>();

		// detect series names and create episode list fetch tasks
		for (String query : names) {
			List<SearchResult> results = db.search(query, locale);

			// select search result
			if (results.size() > 0) {
				List<SearchResult> selectedSearchResults = selectSearchResult(query, results, strict);

				if (selectedSearchResults != null) {
					for (SearchResult it : selectedSearchResults) {
						if (shows.add(it)) {
							try {
								CLILogger.fine(format("Fetching episode data for [%s]", it.getName()));
								episodes.addAll(db.getEpisodeList(it, sortOrder, locale));
								Analytics.trackEvent(db.getName(), "FetchEpisodeList", it.getName());
							} catch (IOException e) {
								CLILogger.log(Level.SEVERE, e.getMessage());
							}
						}
					}
				}
			}
		}

		return episodes;
	}

	public List<File> renameMovie(Collection<File> files, RenameAction renameAction, ConflictAction conflictAction, File outputDir, ExpressionFormat format, MovieIdentificationService service, String query, ExpressionFilter filter, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename movies using [%s]", service.getName()));

		// ignore sample files
		List<File> fileset = filter(files, not(getClutterFileFilter()));

		// handle movie files
		Set<File> movieFiles = new TreeSet<File>(filter(fileset, VIDEO_FILES));
		Set<File> nfoFiles = new TreeSet<File>(filter(fileset, NFO_FILES));

		List<File> orphanedFiles = new ArrayList<File>(filter(fileset, FILES));
		orphanedFiles.removeAll(movieFiles);
		orphanedFiles.removeAll(nfoFiles);

		Map<File, List<File>> derivatesByMovieFile = new HashMap<File, List<File>>();
		for (File movieFile : movieFiles) {
			derivatesByMovieFile.put(movieFile, new ArrayList<File>());
		}
		for (File file : orphanedFiles) {
			List<File> orphanParent = listPath(file);
			for (File movieFile : movieFiles) {
				if (orphanParent.contains(movieFile.getParentFile()) && isDerived(file, movieFile)) {
					derivatesByMovieFile.get(movieFile).add(file);
					break;
				}
			}
		}
		for (List<File> derivates : derivatesByMovieFile.values()) {
			orphanedFiles.removeAll(derivates);
		}

		// match movie hashes online
		final Map<File, Movie> movieByFile = new TreeMap<File, Movie>();
		if (query == null) {
			if (movieFiles.size() > 0) {
				try {
					Map<File, Movie> hashLookup = service.getMovieDescriptors(movieFiles, locale);
					if (hashLookup.size() > 0) {
						CLILogger.finest(format("Looking up up movie by filehash via [%s]", service.getName()));
						movieByFile.putAll(hashLookup);
					}
					Analytics.trackEvent(service.getName(), "HashLookup", "Movie", hashLookup.size()); // number of positive hash lookups
				} catch (UnsupportedOperationException e) {
					// ignore logging => hash lookup only supported by OpenSubtitles
				}
			}

			// collect useful nfo files even if they are not part of the selected fileset
			Set<File> effectiveNfoFileSet = new TreeSet<File>(nfoFiles);
			for (File dir : mapByFolder(movieFiles).keySet()) {
				addAll(effectiveNfoFileSet, dir.listFiles(NFO_FILES));
			}
			for (File dir : filter(fileset, FOLDERS)) {
				addAll(effectiveNfoFileSet, dir.listFiles(NFO_FILES));
			}

			for (File nfo : effectiveNfoFileSet) {
				try {
					Movie movie = grepMovie(nfo, service, locale);

					// ignore illegal nfos
					if (movie == null) {
						continue;
					}

					if (nfoFiles.contains(nfo)) {
						movieByFile.put(nfo, movie);
					}

					if (isDiskFolder(nfo.getParentFile())) {
						// special handling for disk folders
						for (File folder : fileset) {
							if (nfo.getParentFile().equals(folder)) {
								movieByFile.put(folder, movie);
							}
						}
					} else {
						// match movie info to movie files that match the nfo file name
						SortedSet<File> siblingMovieFiles = new TreeSet<File>(filter(movieFiles, new ParentFilter(nfo.getParentFile())));
						String baseName = stripReleaseInfo(getName(nfo)).toLowerCase();

						for (File movieFile : siblingMovieFiles) {
							if (!baseName.isEmpty() && stripReleaseInfo(getName(movieFile)).toLowerCase().startsWith(baseName)) {
								movieByFile.put(movieFile, movie);
							}
						}
					}
				} catch (NoSuchElementException e) {
					CLILogger.warning("Failed to grep IMDbID: " + nfo.getName());
				}
			}
		} else {
			CLILogger.fine(format("Looking up movie by query [%s]", query));
			List<Movie> results = service.searchMovie(query, locale);
			List<Movie> validResults = applyExpressionFilter(results, filter);
			if (validResults.isEmpty()) {
				throw new Exception("Unable to find a valid match: " + results);
			}

			// force all mappings
			Movie result = (Movie) selectSearchResult(query, validResults, strict).get(0);
			for (File file : files) {
				movieByFile.put(file, result);
			}
		}

		// collect files that will be matched one by one
		List<File> movieMatchFiles = new ArrayList<File>();
		movieMatchFiles.addAll(movieFiles);
		movieMatchFiles.addAll(nfoFiles);
		movieMatchFiles.addAll(filter(files, FOLDERS));
		movieMatchFiles.addAll(filter(orphanedFiles, SUBTITLE_FILES)); // run movie detection only on orphaned subtitle files

		// sanity check that we have something to do
		if (fileset.isEmpty() || movieMatchFiles.isEmpty()) {
			throw new Exception("No media files: " + files);
		}

		// map movies to (possibly multiple) files (in natural order)
		Map<Movie, SortedSet<File>> filesByMovie = new HashMap<Movie, SortedSet<File>>();

		// map all files by movie
		for (final File file : movieMatchFiles) {
			Movie movie = movieByFile.get(file);

			// unknown hash, try via imdb id from nfo file
			if (movie == null) {
				CLILogger.fine(format("Auto-detect movie from context: [%s]", file));
				Collection<Movie> results = detectMovie(file, null, service, locale, strict);
				List<Movie> validResults = applyExpressionFilter(results, filter);
				try {
					if (validResults.size() > 0) {
						movie = (Movie) selectSearchResult(query, validResults, strict).get(0);
					}
				} catch (Exception e) {
					CLILogger.log(Level.WARNING, String.format("%s: [%s/%s] %s", e.getClass().getSimpleName(), guessMovieFolder(file) != null ? guessMovieFolder(file).getName() : null, file.getName(), e.getMessage()));
				}

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

		for (Entry<Movie, SortedSet<File>> byMovie : filesByMovie.entrySet()) {
			for (List<File> movieFileListByMediaFolder : mapByMediaFolder(byMovie.getValue()).values()) {
				for (List<File> fileSet : mapByExtension(movieFileListByMediaFolder).values()) {
					// resolve movie parts
					for (int i = 0; i < fileSet.size(); i++) {
						Movie moviePart = byMovie.getKey();
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
		}

		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();

		for (Match<File, ?> match : matches) {
			File file = match.getValue();
			Object movie = match.getCandidate();
			String newName = (format != null) ? format.format(new MediaBindingBean(movie, file, getContext(matches))) : validateFileName(MovieFormat.NameYear.format(movie));

			renameMap.put(file, getDestinationFile(file, newName, outputDir));
		}

		// rename movies
		Analytics.trackEvent("CLI", "Rename", "Movie", renameMap.size());
		return renameAll(renameMap, renameAction, conflictAction, matches);
	}

	public List<File> renameMusic(Collection<File> files, RenameAction renameAction, ConflictAction conflictAction, File outputDir, ExpressionFormat format, MusicIdentificationService service) throws Exception {
		CLILogger.config(format("Rename music using [%s]", service.getName()));
		List<File> audioFiles = filter(files, AUDIO_FILES);

		// check audio files against acoustid
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		for (Entry<File, AudioTrack> it : service.lookup(audioFiles).entrySet()) {
			if (it.getKey() != null && it.getValue() != null) {
				matches.add(new Match<File, AudioTrack>(it.getKey(), it.getValue().clone()));
			}
		}

		// map old files to new paths by applying formatting and validating filenames
		Map<File, File> renameMap = new LinkedHashMap<File, File>();

		for (Match<File, ?> it : matches) {
			File file = it.getValue();
			AudioTrack music = (AudioTrack) it.getCandidate();
			String newName = (format != null) ? format.format(new MediaBindingBean(music, file, getContext(matches))) : validateFileName(music.toString());

			renameMap.put(file, getDestinationFile(file, newName, outputDir));
		}

		// error logging
		if (renameMap.size() != audioFiles.size()) {
			for (File f : audioFiles) {
				if (!renameMap.containsKey(f)) {
					CLILogger.warning("Failed to lookup audio file: " + f.getName());
				}
			}
		}

		// rename movies
		Analytics.trackEvent("CLI", "Rename", "AudioTrack", renameMap.size());
		return renameAll(renameMap, renameAction, conflictAction, null);
	}

	private Map<File, Object> getContext(final Collection<Match<File, ?>> matches) {
		return new AbstractMap<File, Object>() {

			@Override
			public Set<Entry<File, Object>> entrySet() {
				Set<Entry<File, Object>> context = new LinkedHashSet<Entry<File, Object>>();
				for (Match<File, ?> it : matches) {
					if (it.getValue() != null && it.getCandidate() != null) {
						context.add(new SimpleImmutableEntry<File, Object>(it.getValue(), it.getCandidate()));
					}
				}
				return context;
			}
		};
	}

	private File getDestinationFile(File original, String newName, File outputDir) {
		String extension = getExtension(original);
		File newFile = new File(extension != null ? newName + '.' + extension : newName);

		// resolve against output dir
		if (outputDir != null && !newFile.isAbsolute()) {
			newFile = new File(outputDir, newFile.getPath());
		}

		if (isInvalidFilePath(newFile) && !isUnixFS()) {
			CLILogger.config("Stripping invalid characters from new path: " + newName);
			newFile = validateFilePath(newFile);
		}

		return newFile;
	}

	public List<File> renameAll(Map<File, File> renameMap, RenameAction renameAction, ConflictAction conflictAction, List<Match<File, ?>> matches) throws Exception {
		if (renameMap.isEmpty()) {
			throw new Exception(format("[%s] Unable to process any files", renameAction));
		}

		// rename files
		final List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();

		try {
			for (Entry<File, File> it : renameMap.entrySet()) {
				try {
					File source = it.getKey();
					File destination = it.getValue();

					// resolve destination
					if (!destination.isAbsolute()) {
						// same folder, different name
						destination = new File(source.getParentFile(), destination.getPath());
					}

					if (!destination.equals(source) && destination.exists()) {
						if (conflictAction == ConflictAction.FAIL) {
							throw new Exception("File already exists: " + destination);
						}

						if (conflictAction == ConflictAction.OVERRIDE || (conflictAction == ConflictAction.AUTO && VIDEO_SIZE_ORDER.compare(source, destination) > 0)) {
							if (!destination.delete()) {
								CLILogger.log(Level.SEVERE, "Failed to override file: " + destination);
							}
						}
					}

					// rename file, throw exception on failure
					if (!destination.equals(source) && !destination.exists()) {
						CLILogger.info(format("[%s] Rename [%s] to [%s]", renameAction, it.getKey(), it.getValue()));
						destination = renameAction.rename(source, destination);
					} else {
						CLILogger.info(format("Skipped [%s] because [%s] already exists", source, destination));
					}

					// remember successfully renamed matches for history entry and possible revert
					renameLog.add(new SimpleImmutableEntry<File, File>(source, destination));
				} catch (IOException e) {
					CLILogger.warning(format("[%s] Failed to rename [%s]", renameAction, it.getKey()));
					throw e;
				}
			}
		} finally {
			if (renameLog.size() > 0) {
				// update rename history
				HistorySpooler.getInstance().append(renameMap.entrySet());

				// printer number of renamed files if any
				CLILogger.fine(format("Processed %d files", renameLog.size()));
			}
		}

		// write metadata into xattr if xattr is enabled
		if (matches != null && (useExtendedFileAttributes() || useCreationDate())) {
			try {
				for (Match<File, ?> match : matches) {
					File file = match.getValue();
					Object meta = match.getCandidate();
					if (renameMap.containsKey(file) && meta != null) {
						File destination = resolveDestination(file, renameMap.get(file), false);
						if (destination.isFile()) {
							MediaDetection.storeMetaInfo(destination, meta, file.getName(), useExtendedFileAttributes(), useCreationDate());
						}
					}
				}
			} catch (Throwable e) {
				CLILogger.warning("Failed to write xattr: " + e.getMessage());
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
	public List<File> getSubtitles(Collection<File> files, String db, String query, String languageName, String output, String csn, String format, boolean strict) throws Exception {
		final Language language = getLanguage(languageName);
		final Pattern databaseFilter = (db != null) ? Pattern.compile(db, Pattern.CASE_INSENSITIVE) : null;
		final SubtitleNaming naming = getSubtitleNaming(format);

		// when rewriting subtitles to target format an encoding must be defined, default to UTF-8
		final Charset outputEncoding = (csn != null) ? Charset.forName(csn) : (output != null) ? Charset.forName("UTF-8") : null;
		final SubtitleFormat outputFormat = (output != null) ? getSubtitleFormatByName(output) : null;

		// ignore anything that is not a video
		files = filter(files, VIDEO_FILES);

		// ignore clutter files from processing
		files = filter(files, not(getClutterFileFilter()));

		// try to find subtitles for each video file
		List<File> remainingVideos = new ArrayList<File>(files);

		// parallel download
		List<File> subtitleFiles = new ArrayList<File>();

		CLILogger.finest(String.format("Get [%s] subtitles for %d files", language.getName(), remainingVideos.size()));
		if (remainingVideos.isEmpty()) {
			throw new Exception("No video files: " + files);
		}

		// lookup subtitles by hash
		for (VideoHashSubtitleService service : getVideoHashSubtitleServices()) {
			if (remainingVideos.isEmpty() || (databaseFilter != null && !databaseFilter.matcher(service.getName()).matches())) {
				continue;
			}

			try {
				CLILogger.fine("Looking up subtitles by filehash via " + service.getName());
				Map<File, SubtitleDescriptor> subtitles = lookupSubtitleByHash(service, language, remainingVideos);
				Map<File, File> downloads = downloadSubtitleBatch(service.getName(), subtitles, outputFormat, outputEncoding, naming);
				remainingVideos.removeAll(downloads.keySet());
				subtitleFiles.addAll(downloads.values());
			} catch (Exception e) {
				CLILogger.warning("Lookup by hash failed: " + e.getMessage());
			}
		}

		for (SubtitleProvider service : getSubtitleProviders()) {
			if (remainingVideos.isEmpty() || (databaseFilter != null && !databaseFilter.matcher(service.getName()).matches())) {
				continue;
			}

			try {
				CLILogger.fine(format("Looking up subtitles by name via %s", service.getName()));
				Map<File, SubtitleDescriptor> subtitles = new TreeMap<File, SubtitleDescriptor>();
				for (Entry<File, List<SubtitleDescriptor>> it : findSubtitleMatches(service, remainingVideos, language.getName(), query, false, strict).entrySet()) {
					if (it.getValue().size() > 0) {
						subtitles.put(it.getKey(), it.getValue().get(0));
					}
				}
				Map<File, File> downloads = downloadSubtitleBatch(service.getName(), subtitles, outputFormat, outputEncoding, naming);
				remainingVideos.removeAll(downloads.keySet());
				subtitleFiles.addAll(downloads.values());
			} catch (Exception e) {
				CLILogger.warning(format("Search by name failed: %s", e.getMessage()));
			}
		}

		// no subtitles for remaining video files
		for (File it : remainingVideos) {
			CLILogger.warning("No matching subtitles found: " + it);
		}
		if (subtitleFiles.size() > 0) {
			Analytics.trackEvent("CLI", "Download", "Subtitle", subtitleFiles.size());
		}
		return subtitleFiles;
	}

	@Override
	public List<File> getMissingSubtitles(Collection<File> files, String db, String query, final String languageName, String output, String csn, final String format, boolean strict) throws Exception {
		List<File> videoFiles = filter(filter(files, VIDEO_FILES), new FileFilter() {

			// save time on repeating filesystem calls
			private final Map<File, File[]> cache = new HashMap<File, File[]>();

			private final SubtitleNaming naming = getSubtitleNaming(format);

			// get language code suffix for given language (.eng)
			private final String languageCodeSuffix = "." + Language.getISO3LanguageCodeByName(getLanguage(languageName).getName());

			@Override
			public boolean accept(File video) {
				File[] subtitlesByFolder = cache.get(video.getParentFile());
				if (subtitlesByFolder == null) {
					subtitlesByFolder = video.getParentFile().listFiles(SUBTITLE_FILES);
					cache.put(video.getParentFile(), subtitlesByFolder);
				}

				for (File subtitle : subtitlesByFolder) {
					// can't tell which subtitle belongs to which file -> if any subtitles exist skip the whole folder
					if (naming == SubtitleNaming.ORIGINAL) {
						return false;
					} else if (isDerived(subtitle, video)) {
						if (naming == SubtitleNaming.MATCH_VIDEO) {
							return false;
						} else if (subtitle.getName().contains(languageCodeSuffix)) {
							return false;
						}
					}
				}
				return true;
			}
		});

		if (videoFiles.isEmpty()) {
			CLILogger.info("No missing subtitles");
			return emptyList();
		}

		return getSubtitles(videoFiles, db, query, languageName, output, csn, format, strict);
	}

	private SubtitleNaming getSubtitleNaming(String format) {
		SubtitleNaming naming = SubtitleNaming.forName(format);
		if (naming != null) {
			return naming;
		} else {
			return SubtitleNaming.MATCH_VIDEO_ADD_LANGUAGE_TAG;
		}
	}

	private Map<File, File> downloadSubtitleBatch(String service, Map<File, SubtitleDescriptor> subtitles, SubtitleFormat outputFormat, Charset outputEncoding, SubtitleNaming naming) {
		Map<File, File> downloads = new HashMap<File, File>();

		// fetch subtitle
		for (Entry<File, SubtitleDescriptor> it : subtitles.entrySet()) {
			try {
				downloads.put(it.getKey(), downloadSubtitle(it.getValue(), it.getKey(), outputFormat, outputEncoding, naming));
				Analytics.trackEvent(service, "DownloadSubtitle", it.getValue().getLanguageName(), 1);
			} catch (Exception e) {
				CLILogger.warning(format("Failed to download %s: %s", it.getValue().getPath(), e.getMessage()));
			}
		}

		return downloads;
	}

	private File downloadSubtitle(SubtitleDescriptor descriptor, File movieFile, SubtitleFormat outputFormat, Charset outputEncoding, SubtitleNaming naming) throws Exception {
		// fetch subtitle archive
		CLILogger.config(format("Fetching [%s]", descriptor.getPath()));
		MemoryFile subtitleFile = fetchSubtitle(descriptor);

		// subtitle filename is based on movie filename
		String ext = getExtension(subtitleFile.getName());
		ByteBuffer data = subtitleFile.getData();

		if (outputFormat != null || outputEncoding != null) {
			if (outputFormat != null) {
				ext = outputFormat.getFilter().extension(); // adjust extension of the output file
			}

			CLILogger.finest(format("Export [%s] as: %s / %s", subtitleFile.getName(), outputFormat, outputEncoding.displayName(Locale.ROOT)));
			data = exportSubtitles(subtitleFile, outputFormat, 0, outputEncoding);
		}

		File destination = new File(movieFile.getParentFile(), naming.format(movieFile, descriptor, ext));
		CLILogger.info(format("Writing [%s] to [%s]", subtitleFile.getName(), destination.getName()));

		writeFile(data, destination);
		return destination;
	}

	private Map<File, SubtitleDescriptor> lookupSubtitleByHash(VideoHashSubtitleService service, Language language, Collection<File> videoFiles) throws Exception {
		Map<File, SubtitleDescriptor> subtitleByVideo = new TreeMap<File, SubtitleDescriptor>();

		for (Entry<File, List<SubtitleDescriptor>> it : service.getSubtitleList(videoFiles.toArray(new File[0]), language.getName()).entrySet()) {
			// guess best hash match (default order is open bad due to invalid hash links)
			SubtitleDescriptor bestMatch = getBestMatch(it.getKey(), it.getValue(), false);

			if (bestMatch != null) {
				CLILogger.finest(format("Matched [%s] to [%s] via filehash", it.getKey().getName(), bestMatch.getName()));
				subtitleByVideo.put(it.getKey(), bestMatch);
			}
		}

		return subtitleByVideo;
	}

	private <T> List<T> applyExpressionFilter(Collection<T> input, ExpressionFilter filter) throws Exception {
		if (filter == null) {
			return new ArrayList<T>(input);
		}

		CLILogger.fine(String.format("Apply Filter: {%s}", filter.getExpression()));
		List<T> output = new ArrayList<T>(input.size());
		for (T it : input) {
			if (filter.matches(new MediaBindingBean(it, null, null))) {
				CLILogger.finest(String.format("Include [%s]", it));
				output.add(it);
			}
		}
		return output;
	}

	public List<SearchResult> findProbableMatches(final String query, Collection<? extends SearchResult> searchResults, boolean strict) {
		// auto-select most probable search result
		List<SearchResult> probableMatches = new ArrayList<SearchResult>();

		// use name similarity metric
		final SimilarityMetric metric = new NameSimilarityMetric();

		// find probable matches using name similarity > 0.8 (or > 0.6 in non-strict mode)
		for (SearchResult result : searchResults) {
			float f = (query == null) ? 1 : metric.getSimilarity(query, result.getName());
			if (f >= (strict && searchResults.size() > 1 ? 0.8 : 0.6) || ((f >= 0.5 || !strict) && (result.getName().toLowerCase().startsWith(query.toLowerCase())))) {
				if (!probableMatches.contains(result)) {
					probableMatches.add(result);
				}
			}
		}

		// sort results by similarity to query
		if (query != null) {
			sort(probableMatches, new SimilarityComparator(query));
		}
		return probableMatches;
	}

	public List<SearchResult> selectSearchResult(String query, Collection<? extends SearchResult> searchResults, boolean strict) throws Exception {
		List<SearchResult> probableMatches = findProbableMatches(query, searchResults, strict);

		if (probableMatches.isEmpty() || (strict && probableMatches.size() != 1)) {
			// allow single search results to just pass through in non-strict mode even if match confidence is low
			if (searchResults.size() == 1 && !strict) {
				return new ArrayList<SearchResult>(searchResults);
			}

			if (strict) {
				throw new Exception("Multiple options: Force auto-select requires non-strict matching: " + searchResults);
			} else {
				if (searchResults.size() > 5) {
					throw new Exception("Unable to auto-select search result: " + searchResults);
				} else {
					return new ArrayList<SearchResult>(searchResults);
				}
			}
		}

		// return first and only value
		return probableMatches.size() <= 5 ? probableMatches : probableMatches.subList(0, 5); // trust that the correct match is in the Top 3
	}

	private Language getLanguage(String lang) throws Exception {
		// try to look up by language code
		Language language = Language.findLanguage(lang);

		if (language == null) {
			// unable to lookup language
			throw new Exception("Illegal language code: " + lang);
		}

		return language;
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
		// ignore folders and any sort of special files
		files = filter(files, FILES);

		// find common parent folder of all files
		File[] fileList = files.toArray(new File[0]);
		File[][] pathArray = new File[fileList.length][];
		for (int i = 0; i < fileList.length; i++) {
			pathArray[i] = listPath(fileList[i].getParentFile()).toArray(new File[0]);
		}

		CommonSequenceMatcher csm = new CommonSequenceMatcher(null, 0, true);
		File[] common = csm.matchFirstCommonSequence(pathArray);

		if (common == null) {
			throw new Exception("Paths must be on the same filesystem: " + files);
		}

		// last element in the common sequence must be the root folder
		File root = common[common.length - 1];

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

		if (files.isEmpty()) {
			throw new Exception("No files: " + files);
		}

		CLILogger.info(format("Compute %s hash for %s files [%s]", hashType, files.size(), outputFile));
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
		if (query == null || query.isEmpty())
			throw new IllegalArgumentException("query is not defined");

		// find series on the web and fetch episode list
		ExpressionFormat format = (expression != null) ? new ExpressionFormat(expression) : null;
		EpisodeListProvider service = (db == null) ? TheTVDB : getEpisodeListProvider(db);
		SortOrder sortOrder = SortOrder.forName(sortOrderName);
		Locale locale = getLanguage(languageName).getLocale();

		SearchResult hit = selectSearchResult(query, service.search(query, locale), false).get(0);
		List<String> episodes = new ArrayList<String>();

		for (Episode it : service.getEpisodeList(hit, sortOrder, locale)) {
			String name = (format != null) ? format.format(new MediaBindingBean(it, null, null)) : EpisodeFormat.SeasonEpisode.format(it);
			episodes.add(name);
		}

		return episodes;
	}

	@Override
	public String getMediaInfo(File file, String expression) throws Exception {
		ExpressionFormat format = new ExpressionFormat(expression != null ? expression : "{fn} [{resolution} {af} {vc} {ac}]");
		return format.format(new MediaBindingBean(file, file, null));
	}

	@Override
	public List<File> extract(Collection<File> files, String output, String conflict, FileFilter filter, boolean forceExtractAll) throws Exception {
		ConflictAction conflictAction = ConflictAction.forName(conflict);

		// only keep single-volume archives or first part of multi-volume archives
		List<File> archiveFiles = filter(files, Archive.VOLUME_ONE_FILTER);
		List<File> extractedFiles = new ArrayList<File>();

		for (File file : archiveFiles) {
			Archive archive = new Archive(file);
			try {
				File outputFolder = new File(output != null ? output : getName(file));
				if (!outputFolder.isAbsolute()) {
					outputFolder = new File(file.getParentFile(), outputFolder.getPath());
				}

				CLILogger.info(String.format("Read archive [%s] and extract to [%s]", file.getName(), outputFolder));
				final FileMapper outputMapper = new FileMapper(outputFolder, false);

				final List<FileInfo> outputMapping = new ArrayList<FileInfo>();
				for (FileInfo it : archive.listFiles()) {
					File outputPath = outputMapper.getOutputFile(it.toFile());
					outputMapping.add(new SimpleFileInfo(outputPath.getPath(), it.getLength()));
				}

				final Set<FileInfo> selection = new TreeSet<FileInfo>();
				for (FileInfo future : outputMapping) {
					if (filter == null || filter.accept(future.toFile())) {
						selection.add(future);
					}
				}

				// check if there is anything to extract at all
				if (selection.isEmpty()) {
					continue;
				}

				boolean skip = true;
				for (FileInfo future : filter == null || forceExtractAll ? outputMapping : selection) {
					if (conflictAction == ConflictAction.AUTO) {
						skip &= (future.toFile().exists() && future.getLength() == future.toFile().length());
					} else {
						skip &= (future.toFile().exists());
					}
				}

				if (!skip || conflictAction == ConflictAction.OVERRIDE) {
					if (filter == null || forceExtractAll) {
						CLILogger.finest("Extracting files " + outputMapping);

						// extract all files
						archive.extract(outputMapper);

						for (FileInfo it : outputMapping) {
							extractedFiles.add(it.toFile());
						}
					} else {
						CLILogger.finest("Extracting files " + selection);

						// extract files selected by the given filter
						archive.extract(outputMapper, new FileFilter() {

							@Override
							public boolean accept(File entry) {
								return selection.contains(outputMapper.getOutputFile(entry));
							}
						});

						for (FileInfo it : selection) {
							extractedFiles.add(it.toFile());
						}
					}
				} else {
					CLILogger.finest("Skipped extracting files " + selection);
				}
			} finally {
				archive.close();
			}
		}

		return extractedFiles;
	}
}
