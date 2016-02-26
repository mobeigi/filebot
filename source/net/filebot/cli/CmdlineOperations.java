package net.filebot.cli;

import static java.lang.String.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.Settings.*;
import static net.filebot.WebServices.*;
import static net.filebot.cli.CLILogging.*;
import static net.filebot.hash.VerificationUtilities.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.subtitle.SubtitleUtilities.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.StringUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
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
import java.util.stream.IntStream;

import net.filebot.HistorySpooler;
import net.filebot.Language;
import net.filebot.MediaTypes;
import net.filebot.RenameAction;
import net.filebot.StandardRenameAction;
import net.filebot.archive.Archive;
import net.filebot.archive.FileMapper;
import net.filebot.format.ExpressionFileFilter;
import net.filebot.format.ExpressionFilter;
import net.filebot.format.ExpressionFormat;
import net.filebot.format.MediaBindingBean;
import net.filebot.hash.HashType;
import net.filebot.hash.VerificationFileReader;
import net.filebot.hash.VerificationFileWriter;
import net.filebot.media.MediaDetection;
import net.filebot.media.XattrMetaInfoProvider;
import net.filebot.similarity.CommonSequenceMatcher;
import net.filebot.similarity.EpisodeMatcher;
import net.filebot.similarity.Match;
import net.filebot.subtitle.SubtitleFormat;
import net.filebot.subtitle.SubtitleNaming;
import net.filebot.util.EntryList;
import net.filebot.util.FileUtilities;
import net.filebot.util.FileUtilities.ParentFilter;
import net.filebot.vfs.FileInfo;
import net.filebot.vfs.MemoryFile;
import net.filebot.vfs.SimpleFileInfo;
import net.filebot.web.AudioTrack;
import net.filebot.web.Episode;
import net.filebot.web.EpisodeFormat;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.Movie;
import net.filebot.web.MovieFormat;
import net.filebot.web.MovieIdentificationService;
import net.filebot.web.MoviePart;
import net.filebot.web.MusicIdentificationService;
import net.filebot.web.OpenSubtitlesClient;
import net.filebot.web.SearchResult;
import net.filebot.web.SortOrder;
import net.filebot.web.SubtitleDescriptor;
import net.filebot.web.SubtitleProvider;
import net.filebot.web.VideoHashSubtitleService;

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

		if (XattrMetaData.getName().equalsIgnoreCase(db)) {
			return renameByMetaData(files, action, conflictAction, outputDir, format, filter, XattrMetaData);
		}

		// auto-determine mode
		List<File> mediaFiles = filter(files, VIDEO_FILES, SUBTITLE_FILES);
		double max = mediaFiles.size();
		int sxe = 0; // SxE
		int cws = 0; // common word sequence

		Collection<String> cwsList = emptySet();
		if (max >= 5) {
			cwsList = getSeriesNameMatcher(true).matchAll(mediaFiles.toArray(new File[0]));
		}

		for (File f : mediaFiles) {
			// count SxE matches
			if (MediaDetection.getEpisodeIdentifier(f.getName(), true) != null) {
				sxe++;
			}

			// count CWS matches
			for (String base : cwsList) {
				if (base.equalsIgnoreCase(getSeriesNameMatcher(true).matchByFirstCommonWordSequence(base, f.getName()))) {
					cws++;
					break;
				}
			}
		}

		CLILogger.finest(format("Filename pattern: [%.02f] SxE, [%.02f] CWS", sxe / max, cws / max));
		if (sxe > (max * 0.65) || cws > (max * 0.65)) {
			return renameSeries(files, action, conflictAction, outputDir, format, TheTVDB, query, SortOrder.forName(sortOrder), filter, locale, strict); // use default episode db
		} else {
			return renameMovie(files, action, conflictAction, outputDir, format, TheMovieDB, query, filter, locale, strict); // use default movie db
		}
	}

	@Override
	public List<File> rename(Map<File, File> renameMap, RenameAction renameAction, String conflict) throws Exception {
		// generic rename function that can be passed any set of files
		return renameAll(renameMap, renameAction, ConflictAction.forName(conflict), null);
	}

	public List<File> renameSeries(Collection<File> files, RenameAction renameAction, ConflictAction conflictAction, File outputDir, ExpressionFormat format, EpisodeListProvider db, String query, SortOrder sortOrder, ExpressionFilter filter, Locale locale, boolean strict) throws Exception {
		CLILogger.config(format("Rename episodes using [%s]", db.getName()));

		// ignore sample files
		List<File> fileset = sortByUniquePath(filter(files, not(getClutterFileFilter())));

		List<File> mediaFiles = filter(fileset, VIDEO_FILES, SUBTITLE_FILES);
		if (mediaFiles.isEmpty()) {
			throw new CmdlineException("No media files: " + files);
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
					seriesNames = asList(PIPE.split(query));
				}

				if (strict && seriesNames.size() > 1) {
					throw new CmdlineException("Processing multiple shows at once requires -non-strict");
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

				for (List<File> filesPerType : mapByMediaExtension(filter(batch, VIDEO_FILES, SUBTITLE_FILES)).values()) {
					matches.addAll(matchEpisodes(filesPerType, episodes, strict));
				}
			}
		}

		if (matches.isEmpty()) {
			throw new CmdlineException("Unable to match files to episode data");
		}

		// handle derived files
		List<Match<File, ?>> derivateMatches = new ArrayList<Match<File, ?>>();
		SortedSet<File> derivateFiles = new TreeSet<File>(fileset);
		derivateFiles.removeAll(mediaFiles);

		for (File file : derivateFiles) {
			for (Match<File, ?> match : matches) {
				if (file.getPath().startsWith(match.getValue().getParentFile().getPath()) && isDerived(file, match.getValue()) && match.getCandidate() instanceof Episode) {
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
		return renameAll(renameMap, renameAction, conflictAction, matches);
	}

	private List<Match<File, Object>> matchEpisodes(Collection<File> files, Collection<Episode> episodes, boolean strict) throws Exception {
		// always use strict fail-fast matcher
		EpisodeMatcher matcher = new EpisodeMatcher(files, episodes, strict);
		List<Match<File, Object>> matches = matcher.match();

		for (File failedMatch : matcher.remainingValues()) {
			CLILogger.warning("No matching episode: " + failedMatch.getName());
		}

		// in non-strict mode just pass back results as we got it from the matcher
		if (!strict) {
			return matches;
		}

		// in strict mode sanity check the result and only pass back good matches
		List<Match<File, Object>> validMatches = new ArrayList<Match<File, Object>>();
		for (Match<File, Object> it : matches) {
			if (isEpisodeNumberMatch(it.getValue(), (Episode) it.getCandidate())) {
				validMatches.add(it);
			}
		}
		return validMatches;
	}

	private Set<Episode> fetchEpisodeSet(final EpisodeListProvider db, final Collection<String> names, final SortOrder sortOrder, final Locale locale, final boolean strict) throws Exception {
		Set<SearchResult> shows = new LinkedHashSet<SearchResult>();
		Set<Episode> episodes = new LinkedHashSet<Episode>();

		// detect series names and create episode list fetch tasks
		for (String query : names) {
			List<SearchResult> results = db.search(query, locale);

			// select search result
			if (results.size() > 0) {
				List<SearchResult> selectedSearchResults = selectSearchResult(query, results, true, strict);

				if (selectedSearchResults != null) {
					for (SearchResult it : selectedSearchResults) {
						if (shows.add(it)) {
							try {
								CLILogger.fine(format("Fetching episode data for [%s]", it.getName()));
								episodes.addAll(db.getEpisodeList(it, sortOrder, locale));
							} catch (IOException e) {
								throw new CmdlineException(format("Failed to fetch episode data for [%s]: %s", it, e.getMessage()), e);
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
		List<File> fileset = sortByUniquePath(filter(files, not(getClutterFileFilter())));

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
						CLILogger.finest(format("Looking up up movie by hash via [%s]", service.getName()));
						movieByFile.putAll(hashLookup);
					}
				} catch (UnsupportedOperationException e) {
					// ignore logging => hash lookup only supported by OpenSubtitles
				}
			}

			// collect useful nfo files even if they are not part of the selected fileset
			Set<File> effectiveNfoFileSet = new TreeSet<File>(nfoFiles);
			for (File dir : mapByFolder(movieFiles).keySet()) {
				effectiveNfoFileSet.addAll(getChildren(dir, NFO_FILES));
			}
			for (File dir : filter(fileset, FOLDERS)) {
				effectiveNfoFileSet.addAll(getChildren(dir, NFO_FILES));
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
				throw new CmdlineException("Unable to find a valid match: " + results);
			}

			// force all mappings
			Movie result = (Movie) selectSearchResult(query, validResults, false, strict).get(0);
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
			throw new CmdlineException("No media files: " + files);
		}

		// map movies to (possibly multiple) files (in natural order)
		Map<Movie, SortedSet<File>> filesByMovie = new HashMap<Movie, SortedSet<File>>();

		// map all files by movie
		for (final File file : movieMatchFiles) {
			Movie movie = movieByFile.get(file);

			// unknown hash, try via imdb id from nfo file
			if (movie == null) {
				CLILogger.fine(format("Auto-detect movie from context: [%s]", file));
				Collection<Movie> options = detectMovie(file, service, locale, strict);

				// apply filter if defined
				options = applyExpressionFilter(options, filter);

				// reduce options to perfect matches if possible
				List<Movie> perfectMatches = matchMovieByWordSequence(getName(file), options, 0);
				if (perfectMatches.size() > 0) {
					options = perfectMatches;
				}

				try {
					// select first element if matches are reliable
					if (options.size() > 0) {
						// make sure to get the language-specific movie object for the selected option
						movie = service.getMovieDescriptor((Movie) selectSearchResult(null, options, false, strict).get(0), locale);
					}
				} catch (Exception e) {
					CLILogger.log(Level.WARNING, String.format("%s: [%s/%s] %s", e.getClass().getSimpleName(), guessMovieFolder(file) != null ? guessMovieFolder(file).getName() : null, file.getName(), e.getMessage()));
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
		return renameAll(renameMap, renameAction, conflictAction, matches);
	}

	public List<File> renameMusic(Collection<File> files, RenameAction renameAction, ConflictAction conflictAction, File outputDir, ExpressionFormat format, MusicIdentificationService service) throws Exception {
		CLILogger.config(format("Rename music using [%s]", service.getName()));
		List<File> audioFiles = sortByUniquePath(filter(files, AUDIO_FILES, VIDEO_FILES));

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
					CLILogger.warning(String.format("Unable to lookup %s: %s", service.getName(), f.getName()));
				}
			}
		}

		// rename movies
		return renameAll(renameMap, renameAction, conflictAction, null);
	}

	public List<File> renameByMetaData(Collection<File> files, RenameAction renameAction, ConflictAction conflictAction, File outputDir, ExpressionFormat format, ExpressionFilter filter, XattrMetaInfoProvider service) throws Exception {
		CLILogger.config(format("Rename files using [%s]", service.getName()));

		// force sort order
		List<File> selection = sortByUniquePath(files);
		Map<File, File> renameMap = new LinkedHashMap<File, File>();

		for (Entry<File, Object> it : service.getMetaData(selection).entrySet()) {
			MediaBindingBean bindingBean = new MediaBindingBean(it.getValue(), it.getKey());

			if (filter == null || filter.matches(bindingBean)) {
				String newName = (format != null) ? format.format(bindingBean) : validateFileName(it.getValue().toString());
				renameMap.put(it.getKey(), getDestinationFile(it.getKey(), newName, outputDir));
			}
		}

		// rename files according to xattr metadata objects
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
		File newFile = new File(extension != null ? newName + '.' + extension.toLowerCase() : newName);

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
			throw new CmdlineException("Unable to identify or process any files");
		}

		// rename files
		Map<File, File> renameLog = new LinkedHashMap<File, File>();

		try {
			for (Entry<File, File> it : renameMap.entrySet()) {
				try {
					File source = it.getKey();
					File destination = it.getValue();

					// resolve destination
					if (!destination.isAbsolute()) {
						// same folder, different name
						destination = resolveDestination(source, destination, false);
					}

					if (!destination.equals(source) && destination.exists() && renameAction != StandardRenameAction.TEST) {
						if (conflictAction == ConflictAction.FAIL) {
							throw new CmdlineException("File already exists: " + destination);
						}

						if (conflictAction == ConflictAction.OVERRIDE || (conflictAction == ConflictAction.AUTO && VIDEO_SIZE_ORDER.compare(source, destination) > 0)) {
							if (!destination.delete()) {
								CLILogger.log(Level.SEVERE, "Failed to override file: " + destination);
							}
						} else if (conflictAction == ConflictAction.INDEX) {
							destination = nextAvailableIndexedName(destination);
						}
					}

					// rename file, throw exception on failure
					if (!destination.equals(source) && !destination.exists()) {
						CLILogger.info(format("[%s] Rename [%s] to [%s]", renameAction, source, destination));
						destination = renameAction.rename(source, destination);

						// remember successfully renamed matches for history entry and possible revert
						renameLog.put(source, destination);
					} else {
						CLILogger.info(format("Skipped [%s] because [%s] already exists", source, destination));
					}
				} catch (IOException e) {
					CLILogger.warning(format("[%s] Failed to rename [%s]", renameAction, it.getKey()));
					throw e;
				}
			}
		} finally {
			// update rename history
			HistorySpooler.getInstance().append(renameLog.entrySet());

			// printer number of renamed files if any
			CLILogger.fine(format("Processed %d files", renameLog.size()));
		}

		// write metadata into xattr if xattr is enabled
		if (matches != null && renameLog.size() > 0 && (useExtendedFileAttributes() || useCreationDate()) && renameAction != StandardRenameAction.TEST) {
			try {
				for (Match<File, ?> match : matches) {
					File source = match.getValue();
					Object infoObject = match.getCandidate();
					if (infoObject != null) {
						File destination = renameLog.get(source);
						if (destination != null && destination.isFile()) {
							MediaDetection.storeMetaInfo(destination, infoObject, source.getName(), useExtendedFileAttributes(), useCreationDate());
						}
					}
				}
			} catch (Throwable e) {
				CLILogger.warning("Failed to write xattr: " + e.getMessage());
			}
		}

		// new file names
		return new ArrayList<File>(renameLog.values());
	}

	private static File nextAvailableIndexedName(File file) {
		File parent = file.getParentFile();
		String name = getName(file);
		String ext = getExtension(file);
		return IntStream.range(1, 100).mapToObj(i -> new File(parent, name + '.' + i + '.' + ext)).filter(f -> !f.exists()).findFirst().get();
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

		// ignore sample files
		files = sortByUniquePath(filter(files, not(getClutterFileFilter())));

		// try to find subtitles for each video file
		List<File> remainingVideos = new ArrayList<File>(files);

		// parallel download
		List<File> subtitleFiles = new ArrayList<File>();

		CLILogger.finest(String.format("Get [%s] subtitles for %d files", language.getName(), remainingVideos.size()));
		if (remainingVideos.isEmpty()) {
			throw new CmdlineException("No video files: " + files);
		}

		// lookup subtitles by hash
		for (VideoHashSubtitleService service : getVideoHashSubtitleServices(language.getLocale())) {
			if (remainingVideos.isEmpty() || (databaseFilter != null && !databaseFilter.matcher(service.getName()).matches()) || !requireLogin(service)) {
				continue;
			}

			try {
				CLILogger.fine("Looking up subtitles by hash via " + service.getName());
				Map<File, SubtitleDescriptor> subtitles = lookupSubtitleByHash(service, language, remainingVideos, strict);
				Map<File, File> downloads = downloadSubtitleBatch(service.getName(), subtitles, outputFormat, outputEncoding, naming);
				remainingVideos.removeAll(downloads.keySet());
				subtitleFiles.addAll(downloads.values());
			} catch (Exception e) {
				CLILogger.warning("Lookup by hash failed: " + e.getMessage());
			}
		}

		for (SubtitleProvider service : getSubtitleProviders()) {
			if (strict || remainingVideos.isEmpty() || (databaseFilter != null && !databaseFilter.matcher(service.getName()).matches()) || !requireLogin(service)) {
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
		return subtitleFiles;
	}

	private static boolean requireLogin(Object service) {
		if (service instanceof OpenSubtitlesClient) {
			OpenSubtitlesClient osdb = (OpenSubtitlesClient) service;
			if (osdb.isAnonymous()) {
				throw new CmdlineException(String.format("%s: Please enter your login details by calling `filebot -script fn:configure`", osdb.getName()));
			}
		}
		return true; // no login => logged in by default
	}

	@Override
	public List<File> getMissingSubtitles(Collection<File> files, String db, String query, final String languageName, String output, String csn, final String format, boolean strict) throws Exception {
		List<File> videoFiles = filter(filter(files, VIDEO_FILES), new FileFilter() {

			// save time on repeating filesystem calls
			private final Map<File, List<File>> cache = new HashMap<File, List<File>>();

			private final SubtitleNaming naming = getSubtitleNaming(format);

			// get language code suffix for given language (.eng)
			private final String languageCode = Language.getStandardLanguageCode(getLanguage(languageName).getName());

			public boolean matchesLanguageCode(File f) {
				Locale languageSuffix = MediaDetection.releaseInfo.getLanguageSuffix(FileUtilities.getName(f));
				Language language = Language.getLanguage(languageSuffix);
				if (language != null) {
					return language.getISO3().equalsIgnoreCase(languageCode);
				}
				return false;
			}

			@Override
			public boolean accept(File video) {
				List<File> subtitlesByFolder = cache.get(video.getParentFile());
				if (subtitlesByFolder == null) {
					subtitlesByFolder = getChildren(video.getParentFile(), SUBTITLE_FILES);
					cache.put(video.getParentFile(), subtitlesByFolder);
				}

				boolean accept = true;
				for (File subtitle : subtitlesByFolder) {
					// can't tell which subtitle belongs to which file -> if any subtitles exist skip the whole folder
					if (naming == SubtitleNaming.ORIGINAL) {
						return false;
					} else if (isDerived(subtitle, video)) {
						if (naming == SubtitleNaming.MATCH_VIDEO) {
							return false;
						} else {
							accept &= !matchesLanguageCode(subtitle);
						}
					}
				}
				return accept;
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

	private Map<File, SubtitleDescriptor> lookupSubtitleByHash(VideoHashSubtitleService service, Language language, Collection<File> videoFiles, boolean strict) throws Exception {
		Map<File, SubtitleDescriptor> subtitleByVideo = new TreeMap<File, SubtitleDescriptor>();

		for (Entry<File, List<SubtitleDescriptor>> it : service.getSubtitleList(videoFiles.toArray(new File[0]), language.getName()).entrySet()) {
			// guess best hash match (default order is open bad due to invalid hash links)
			SubtitleDescriptor bestMatch = getBestMatch(it.getKey(), it.getValue(), strict);

			if (bestMatch != null) {
				CLILogger.finest(format("Matched [%s] to [%s] via hash", it.getKey().getName(), bestMatch.getName()));
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
		Map<File, Object> context = new EntryList<File, Object>(null, input);
		List<T> output = new ArrayList<T>(input.size());
		for (T it : input) {
			if (filter.matches(new MediaBindingBean(it, null, context))) {
				CLILogger.finest(String.format("Include [%s]", it));
				output.add(it);
			}
		}
		return output;
	}

	private List<SearchResult> selectSearchResult(String query, Collection<? extends SearchResult> options, boolean alias, boolean strict) throws Exception {
		List<SearchResult> probableMatches = getProbableMatches(query, options, alias, strict);

		if (probableMatches.isEmpty() || (strict && probableMatches.size() != 1)) {
			// allow single search results to just pass through in non-strict mode even if match confidence is low
			if (options.size() == 1 && !strict) {
				return new ArrayList<SearchResult>(options);
			}

			if (strict) {
				throw new CmdlineException("Multiple options: Force auto-select requires non-strict matching: " + options);
			}

			// just pick the best 5 matches
			if (query != null) {
				probableMatches = new ArrayList<SearchResult>(sortBySimilarity(options, singleton(query), getSeriesMatchMetric()));
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
			throw new CmdlineException("Illegal language code: " + lang);
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
			throw new CmdlineException("Paths must be on the same filesystem: " + files);
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
			throw new CmdlineException("Illegal output type: " + output);
		}

		if (files.isEmpty()) {
			throw new CmdlineException("No files: " + files);
		}

		CLILogger.info(format("Compute %s hash for %s files [%s]", hashType, files.size(), outputFile));
		compute(root.getPath(), files, outputFile, hashType, csn);

		return outputFile;
	}

	private boolean check(File verificationFile, File root) throws Exception {
		HashType type = getHashType(verificationFile);

		// check if type is supported
		if (type == null) {
			throw new CmdlineException("Unsupported format: " + verificationFile);
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
	public List<String> fetchEpisodeList(String query, String expression, String db, String sortOrderName, String filterExpression, String languageName) throws Exception {
		if (query == null || query.isEmpty())
			throw new IllegalArgumentException("query is not defined");

		// find series on the web and fetch episode list
		ExpressionFormat format = (expression != null) ? new ExpressionFormat(expression) : null;
		ExpressionFilter filter = (filterExpression != null) ? new ExpressionFilter(filterExpression) : null;
		EpisodeListProvider service = (db == null) ? TheTVDB : getEpisodeListProvider(db);
		SortOrder sortOrder = SortOrder.forName(sortOrderName);
		Locale locale = getLanguage(languageName).getLocale();

		// fetch episode data
		SearchResult hit = selectSearchResult(query, service.search(query, locale), false, false).get(0);
		List<Episode> episodes = service.getEpisodeList(hit, sortOrder, locale);

		// apply filter
		episodes = applyExpressionFilter(episodes, filter);

		List<String> names = new ArrayList<String>();
		for (Episode it : episodes) {
			String name = (format != null) ? format.format(new MediaBindingBean(it, null, null)) : EpisodeFormat.SeasonEpisode.format(it);
			names.add(name);
		}
		return names;
	}

	@Override
	public List<String> getMediaInfo(Collection<File> files, String format, String filter) throws Exception {
		if (filter != null && filter.length() > 0) {
			ExpressionFileFilter includes = new ExpressionFileFilter(new ExpressionFilter(filter), false);
			files = filter(files, includes);

			if (files.isEmpty()) {
				throw new CmdlineException("No files: " + files);
			}
		}

		ExpressionFormat formatter = new ExpressionFormat(format != null && format.length() > 0 ? format : "{fn} [{resolution} {vc} {channels} {ac} {minutes+'m'}]");
		List<String> output = new ArrayList<String>();
		for (File file : files) {
			String line = formatter.format(new MediaBindingBean(readMetaInfo(file), file, null));
			output.add(line);
		}
		return output;
	}

	@Override
	public List<File> extract(Collection<File> files, String output, String conflict, FileFilter filter, boolean forceExtractAll) throws Exception {
		ConflictAction conflictAction = ConflictAction.forName(conflict);

		// only keep single-volume archives or first part of multi-volume archives
		List<File> archiveFiles = filter(files, Archive.VOLUME_ONE_FILTER);
		List<File> extractedFiles = new ArrayList<File>();

		for (File file : archiveFiles) {
			Archive archive = Archive.open(file);
			try {
				File outputFolder = new File(output != null ? output : getName(file));
				if (!outputFolder.isAbsolute()) {
					outputFolder = new File(file.getParentFile(), outputFolder.getPath());
				}

				CLILogger.info(String.format("Read archive [%s] and extract to [%s]", file.getName(), outputFolder));
				final FileMapper outputMapper = new FileMapper(outputFolder);

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
						archive.extract(outputMapper.getOutputDir());

						for (FileInfo it : outputMapping) {
							extractedFiles.add(it.toFile());
						}
					} else {
						CLILogger.finest("Extracting files " + selection);

						// extract files selected by the given filter
						archive.extract(outputMapper.getOutputDir(), new FileFilter() {

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
