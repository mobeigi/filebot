package net.sourceforge.filebot.media;

import static java.util.Collections.*;
import static java.util.regex.Pattern.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.similarity.CommonSequenceMatcher.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.CollationKey;
import java.text.Collator;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.archive.Archive;
import net.sourceforge.filebot.similarity.CommonSequenceMatcher;
import net.sourceforge.filebot.similarity.DateMatcher;
import net.sourceforge.filebot.similarity.DateMetric;
import net.sourceforge.filebot.similarity.MetricAvg;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.NumericSimilarityMetric;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SeasonEpisodePattern;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.similarity.SequenceMatchSimilarity;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityComparator;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo;
import net.sourceforge.filebot.web.TheTVDBClient.SeriesInfo;
import net.sourceforge.filebot.web.TheTVDBSearchResult;

public class MediaDetection {

	public static final ReleaseInfo releaseInfo = new ReleaseInfo();

	private static FileFilter diskFolder;
	private static FileFilter clutterFile;

	public static FileFilter getDiskFolderFilter() {
		if (diskFolder == null) {
			diskFolder = releaseInfo.getDiskFolderFilter();
		}
		return diskFolder;
	}

	public static FileFilter getClutterFileFilter() throws IOException {
		if (clutterFile == null) {
			clutterFile = releaseInfo.getClutterFileFilter();
		}
		return clutterFile;
	}

	public static boolean isDiskFolder(File folder) {
		return getDiskFolderFilter().accept(folder);
	}

	public static boolean isClutterFile(File file) throws IOException {
		return getClutterFileFilter().accept(file);
	}

	public static boolean isVideoDiskFile(File file) throws Exception {
		FileFilter diskFolderEntryFilter = releaseInfo.getDiskFolderEntryFilter();
		Archive iso = new Archive(file);
		try {
			for (File path : iso.listFiles()) {
				for (File entry : listPath(path)) {
					if (diskFolderEntryFilter.accept(entry)) {
						return true;
					}
				}
			}
			return false;
		} finally {
			iso.close();
		}
	}

	public static Locale guessLanguageFromSuffix(File file) {
		return releaseInfo.getLanguageSuffix(getName(file));
	}

	public static boolean isEpisode(String name, boolean strict) {
		return parseEpisodeNumber(name, strict) != null || parseDate(name) != null;
	}

	public static List<SxE> parseEpisodeNumber(String string, boolean strict) {
		return new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, strict).match(string);
	}

	public static List<SxE> parseEpisodeNumber(File file, boolean strict) {
		return new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, strict).match(file);
	}

	public static Date parseDate(Object object) {
		return new DateMetric().parse(object);
	}

	public static Map<Set<File>, Set<String>> mapSeriesNamesByFiles(Collection<File> files, Locale locale) throws Exception {
		// map series names by folder
		Map<File, Set<String>> seriesNamesByFolder = new HashMap<File, Set<String>>();
		Map<File, List<File>> filesByFolder = mapByFolder(files);

		for (Entry<File, List<File>> it : filesByFolder.entrySet()) {
			Set<String> namesForFolder = new TreeSet<String>(getLenientCollator(locale));
			namesForFolder.addAll(detectSeriesNames(it.getValue(), locale));

			seriesNamesByFolder.put(it.getKey(), namesForFolder);
		}

		// reverse map folders by series name
		Map<String, Set<File>> foldersBySeriesName = new HashMap<String, Set<File>>();

		for (Set<String> nameSet : seriesNamesByFolder.values()) {
			for (String name : nameSet) {
				Set<File> foldersForSeries = new HashSet<File>();
				for (Entry<File, Set<String>> it : seriesNamesByFolder.entrySet()) {
					if (it.getValue().contains(name)) {
						foldersForSeries.add(it.getKey());
					}
				}
				foldersBySeriesName.put(name, foldersForSeries);
			}
		}

		// join both sets
		Map<Set<File>, Set<String>> batchSets = new HashMap<Set<File>, Set<String>>();

		while (seriesNamesByFolder.size() > 0) {
			Set<String> combinedNameSet = new TreeSet<String>(getLenientCollator(locale));
			Set<File> combinedFolderSet = new HashSet<File>();

			// build combined match set
			combinedFolderSet.add(seriesNamesByFolder.keySet().iterator().next());

			boolean resolveFurther = true;
			while (resolveFurther) {
				boolean modified = false;
				for (File folder : combinedFolderSet) {
					modified |= combinedNameSet.addAll(seriesNamesByFolder.get(folder));
				}
				for (String name : combinedNameSet) {
					modified |= combinedFolderSet.addAll(foldersBySeriesName.get(name));
				}
				resolveFurther &= modified;
			}

			// build result entry
			Set<File> combinedFileSet = new TreeSet<File>();
			for (File folder : combinedFolderSet) {
				combinedFileSet.addAll(filesByFolder.get(folder));
			}

			if (combinedFileSet.size() > 0) {
				// divide file set per complete series set
				Map<Object, List<File>> filesByEpisode = new LinkedHashMap<Object, List<File>>();
				for (File file : combinedFileSet) {
					Object eid = getEpisodeIdentifier(file.getName(), true);

					// SPECIAL CASE: 101, 201, 202, etc 3-digit SxE pattern
					if (eid == null) {
						List<SxE> d3sxe = new SeasonEpisodePattern(null, "(?<!\\p{Alnum})(\\d)(\\d{2})(?!\\p{Alnum})").match(file.getName());
						if (d3sxe != null && d3sxe.size() > 0) {
							eid = d3sxe;
						}
					}

					// merge specials into first SxE group
					if (eid == null) {
						eid = file; // open new SxE group for each unrecognized file
					}

					List<File> episodeFiles = filesByEpisode.get(eid);
					if (episodeFiles == null) {
						episodeFiles = new ArrayList<File>();
						filesByEpisode.put(eid, episodeFiles);
					}
					episodeFiles.add(file);
				}

				for (int i = 0; true; i++) {
					Set<File> series = new LinkedHashSet<File>();
					for (List<File> episode : filesByEpisode.values()) {
						if (i < episode.size()) {
							series.add(episode.get(i));
						}
					}

					if (series.isEmpty()) {
						break;
					}

					combinedFileSet.removeAll(series);
					batchSets.put(series, combinedNameSet);
				}

				if (combinedFileSet.size() > 0) {
					batchSets.put(combinedFileSet, combinedNameSet);
				}
			}

			// set folders as accounted for
			seriesNamesByFolder.keySet().removeAll(combinedFolderSet);
		}

		// handle files that have not been matched to a batch set yet
		Set<File> remainingFiles = new HashSet<File>(files);
		for (Set<File> batch : batchSets.keySet()) {
			remainingFiles.removeAll(batch);
		}
		if (remainingFiles.size() > 0) {
			batchSets.put(remainingFiles, null);
		}

		return batchSets;
	}

	public static Object getEpisodeIdentifier(CharSequence name, boolean strict) {
		// check SxE first
		Object match = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, strict).match(name);

		// then Date pattern
		if (match == null)
			match = new DateMatcher().match(name);

		return match;
	}

	public static List<String> detectSeriesNames(Collection<File> files, Locale locale) throws Exception {
		List<String> names = new ArrayList<String>();

		// try xattr metadata if enabled
		if (useExtendedFileAttributes()) {
			try {
				for (File it : files) {
					MetaAttributes xattr = new MetaAttributes(it);
					try {
						Episode episode = (Episode) xattr.getObject();
						names.add(episode.getSeriesName());
					} catch (Exception e) {
						// can't read meta attributes => ignore
					}
				}
			} catch (Throwable e) {
				// ignore
			}
		}

		// try to detect series name via nfo files
		try {
			for (SearchResult it : lookupSeriesNameByInfoFile(files, locale)) {
				names.add(it.getName());
			}
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to lookup info by id: " + e.getMessage(), e);
		}

		// try to detect series name via known patterns
		try {
			names.addAll(matchSeriesByDirectMapping(files));
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to match direct mappings: " + e.getMessage(), e);
		}

		// cross-reference known series names against file structure
		try {
			Set<String> folders = new LinkedHashSet<String>();
			Set<String> filenames = new LinkedHashSet<String>();
			for (File f : files) {
				for (int i = 0; i < 3 && f != null && f.getParentFile() != null; i++, f = f.getParentFile()) {
					(i == 0 ? filenames : folders).add(normalizeBrackets(getName(f)));
				}
			}

			// check foldernames first
			List<String> matches = matchSeriesByName(folders, 0);

			// check all filenames if necessary
			if (matches.isEmpty()) {
				matches.addAll(matchSeriesByName(filenames, 0));
				matches.addAll(matchSeriesByName(stripReleaseInfo(filenames, false), 0));
			}

			// use lenient sub sequence matching only as fallback and try name without spacing logic that may mess up any lookup
			if (matches.isEmpty()) {
				// try to narrow down file to series name as best as possible
				SeriesNameMatcher snm = new SeriesNameMatcher();
				List<String> sns = new ArrayList<String>();
				sns.addAll(folders);
				sns.addAll(filenames);
				for (int i = 0; i < sns.size(); i++) {
					String sn = snm.matchByEpisodeIdentifier(sns.get(i));
					if (sn != null) {
						sns.set(i, sn);
					}
				}
				for (SearchResult it : matchSeriesFromStringWithoutSpacing(stripReleaseInfo(sns, false), true)) {
					matches.add(it.getName());
				}

				// less reliable CWS deep matching
				matches.addAll(matchSeriesByName(folders, 2));
				matches.addAll(matchSeriesByName(filenames, 2));
			}

			// pass along only valid terms
			names.addAll(stripBlacklistedTerms(matches));
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to match folder structure: " + e.getMessage(), e);
		}

		// match common word sequence and clean detected word sequence from unwanted elements
		Collection<String> matches = new LinkedHashSet<String>();

		// check CWS matches
		SeriesNameMatcher snm = new SeriesNameMatcher(locale);
		matches.addAll(snm.matchAll(files.toArray(new File[files.size()])));

		// check for known pattern matches
		for (File f : files) {
			String sn = snm.matchByEpisodeIdentifier(getName(f.getParentFile()));
			if (sn != null) {
				matches.add(sn);
			}
		}

		try {
			Collection<String> priorityMatchSet = new LinkedHashSet<String>();
			priorityMatchSet.addAll(stripReleaseInfo(matches, true));
			priorityMatchSet.addAll(stripReleaseInfo(matches, false));
			matches = priorityMatchSet;
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to clean matches: " + e.getMessage(), e);
		}
		names.addAll(matches);

		// don't allow duplicates
		return getUniqueQuerySet(names);
	}

	public static List<String> matchSeriesByDirectMapping(Collection<File> files) throws Exception {
		Map<Pattern, String> seriesDirectMappings = releaseInfo.getSeriesDirectMappings();
		List<String> matches = new ArrayList<String>();

		for (File file : files) {
			for (Entry<Pattern, String> it : seriesDirectMappings.entrySet()) {
				if (it.getKey().matcher(getName(file)).find()) {
					matches.add(it.getValue());
				}
			}
		}

		return matches;
	}

	private static List<Entry<String, SearchResult>> seriesIndex = new ArrayList<Entry<String, SearchResult>>(75000);

	public static synchronized List<Entry<String, SearchResult>> getSeriesIndex() throws IOException {
		if (seriesIndex.isEmpty()) {
			try {
				for (SearchResult[] index : new SearchResult[][] { releaseInfo.getTheTVDBIndex(), releaseInfo.getAnidbIndex() }) {
					for (SearchResult item : index) {
						for (String name : item.getNames()) {
							seriesIndex.add(new SimpleEntry<String, SearchResult>(normalizePunctuation(name).toLowerCase(), item));
						}
					}
				}
			} catch (Exception e) {
				// can't load movie index, just try again next time
				Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.SEVERE, "Failed to load series index: " + e.getMessage(), e);
				return emptyList();
			}
		}

		return seriesIndex;
	}

	public static List<String> matchSeriesByName(Collection<String> names, int maxStartIndex) throws Exception {
		HighPerformanceMatcher nameMatcher = new HighPerformanceMatcher(maxStartIndex);
		List<String> matches = new ArrayList<String>();

		for (String name : names) {
			String bestMatch = "";
			for (Entry<String, SearchResult> it : getSeriesIndex()) {
				String identifier = it.getKey();
				String commonName = nameMatcher.matchFirstCommonSequence(name, identifier);
				if (commonName != null && commonName.length() >= identifier.length() && commonName.length() > bestMatch.length()) {
					bestMatch = commonName;
				}
			}
			if (bestMatch.length() > 0) {
				matches.add(bestMatch);
			}
		}

		// sort by length of name match (descending)
		sort(matches, new Comparator<String>() {

			@Override
			public int compare(String a, String b) {
				return Integer.valueOf(b.length()).compareTo(Integer.valueOf(a.length()));
			}
		});

		return matches;
	}

	public static List<SearchResult> matchSeriesFromStringWithoutSpacing(Collection<String> names, boolean strict) throws IOException {
		// clear name of punctuation, spacing, and leading 'The' or 'A' that are common causes for word-lookup to fail
		Pattern spacing = Pattern.compile("(^(?i)(The|A)\\b)|[\\p{Punct}\\p{Space}]+");

		List<String> terms = new ArrayList<String>(names.size());
		for (String it : names) {
			String term = spacing.matcher(it).replaceAll("").toLowerCase();
			if (term.length() >= 3) {
				terms.add(term); // only consider words, not just random letters
			}
		}

		// similarity threshold based on strict/non-strict
		SimilarityMetric metric = new NameSimilarityMetric();
		float similarityThreshold = strict ? 0.75f : 0.5f;

		List<SearchResult> seriesList = new ArrayList<SearchResult>();
		for (Entry<String, SearchResult> it : getSeriesIndex()) {
			String name = spacing.matcher(it.getKey()).replaceAll("").toLowerCase();
			for (String term : terms) {
				if (term.contains(name)) {
					if (metric.getSimilarity(term, name) >= similarityThreshold) {
						seriesList.add(it.getValue());
					}
					break;
				}
			}
		}
		return seriesList;
	}

	public static Collection<Movie> detectMovie(File movieFile, MovieIdentificationService hashLookupService, MovieIdentificationService queryLookupService, Locale locale, boolean strict) throws Exception {
		Set<Movie> options = new LinkedHashSet<Movie>();

		// try xattr metadata if enabled
		if (useExtendedFileAttributes()) {
			try {
				MetaAttributes xattr = new MetaAttributes(movieFile);
				try {
					Movie movie = (Movie) xattr.getObject();
					if (movie != null) {
						options.add(new Movie(movie)); // normalize as movie object
					}
				} catch (Exception e) {
					// can't read meta attributes => ignore
				}
			} catch (Throwable e) {
				// ignore
			}
		}

		// lookup by file hash
		if (hashLookupService != null && movieFile.isFile()) {
			try {
				for (Movie movie : hashLookupService.getMovieDescriptors(singleton(movieFile), locale).values()) {
					if (movie != null) {
						options.add(movie);
					}
				}
			} catch (Exception e) {
				Logger.getLogger(MediaDetection.class.getName()).log(Level.WARNING, hashLookupService.getName() + ": " + e.getMessage());
			}
		}

		// lookup by id from nfo file
		if (queryLookupService != null) {
			for (int imdbid : grepImdbId(movieFile.getPath())) {
				Movie movie = queryLookupService.getMovieDescriptor(imdbid, locale);
				if (movie != null) {
					options.add(movie);
				}
			}

			// try to grep imdb id from nfo files
			for (int imdbid : grepImdbIdFor(movieFile)) {
				Movie movie = queryLookupService.getMovieDescriptor(imdbid, locale);
				if (movie != null) {
					options.add(movie);
				}
			}
		}

		// search by file name or folder name
		Collection<String> terms = new LinkedHashSet<String>();

		// 1. term: try to match movie pattern 'name (year)' or use filename as is
		terms.add(getName(movieFile));

		// 2. term: first meaningful parent folder
		File movieFolder = guessMovieFolder(movieFile);
		if (movieFolder != null) {
			terms.add(getName(movieFolder));
		}

		// reduce movie names
		terms = new LinkedHashSet<String>(reduceMovieNamePermutations(terms));

		List<Movie> movieNameMatches = matchMovieName(terms, true, 0);
		if (movieNameMatches.isEmpty()) {
			movieNameMatches = matchMovieName(terms, strict, 2);
		}

		// skip further queries if collected matches are already sufficient
		if (options.size() > 0 && movieNameMatches.size() > 0) {
			options.addAll(movieNameMatches);
			return sortBySimilarity(options, terms);
		}

		// if matching name+year failed, try matching only by name
		if (movieNameMatches.isEmpty() && strict) {
			movieNameMatches = matchMovieName(terms, false, 0);
			if (movieNameMatches.isEmpty()) {
				movieNameMatches = matchMovieName(terms, false, 2);
			}
		}

		// assume name without spacing will mess up any lookup
		if (movieNameMatches.isEmpty()) {
			movieNameMatches = matchMovieFromStringWithoutSpacing(terms, strict);

			if (movieNameMatches.isEmpty() && !terms.equals(stripReleaseInfo(terms, true))) {
				movieNameMatches = matchMovieFromStringWithoutSpacing(stripReleaseInfo(terms, true), strict);
			}
		}

		// query by file / folder name
		if (queryLookupService != null) {
			Collection<Movie> results = queryMovieByFileName(terms, queryLookupService, locale);

			// try query without year as it sometimes messes up results if years don't match properly (movie release years vs dvd release year, etc)
			if (results.isEmpty() && !strict) {
				List<String> lastResortQueryList = new ArrayList<String>();
				Pattern yearPattern = Pattern.compile("(?:19|20)\\d{2}");
				Pattern akaPattern = Pattern.compile("\\bAKA\\b", Pattern.CASE_INSENSITIVE);
				for (String term : terms) {
					if (yearPattern.matcher(term).find() || akaPattern.matcher(term).find()) {
						// try to separate AKA titles as well into separate searches
						for (String mn : akaPattern.split(yearPattern.matcher(term).replaceAll(""))) {
							lastResortQueryList.add(mn.trim());
						}
					}
				}
				if (lastResortQueryList.size() > 0) {
					results = queryMovieByFileName(lastResortQueryList, queryLookupService, locale);
				}
			}

			options.addAll(results);
		}

		// add local matching after online search
		options.addAll(movieNameMatches);

		// sort by relevance
		return sortBySimilarity(options, terms);
	}

	public static SimilarityMetric getMovieMatchMetric() {
		return new MetricAvg(new SequenceMatchSimilarity(), new NameSimilarityMetric(), new SequenceMatchSimilarity(0, true), new NumericSimilarityMetric() {

			private Pattern year = Pattern.compile("\\b\\d{4}\\b");

			@Override
			protected String normalize(Object object) {
				Matcher ym = year.matcher(object.toString());
				StringBuilder sb = new StringBuilder();
				while (ym.find()) {
					sb.append(ym.group()).append(' ');
				}
				return sb.toString().trim();
			}

			@Override
			public float getSimilarity(Object o1, Object o2) {
				return super.getSimilarity(o1, o2) * 2; // DOUBLE WEIGHT FOR YEAR MATCH
			}
		});
	}

	public static <T> List<T> sortBySimilarity(Collection<T> options, Collection<String> terms) throws IOException {
		List<String> paragon = stripReleaseInfo(terms, true);
		List<T> sorted = new ArrayList<T>(options);
		sort(sorted, new SimilarityComparator(getMovieMatchMetric(), paragon.toArray()));

		// DEBUG
		// System.out.format("sortBySimilarity %s => %s", terms, sorted);

		return sorted;
	}

	public static String reduceMovieName(String name, boolean strict) throws IOException {
		Matcher matcher = compile(strict ? "^(.+)[\\[\\(]((?:19|20)\\d{2})[\\]\\)]" : "^(.+?)((?:19|20)\\d{2})").matcher(name);
		if (matcher.find()) {
			return String.format("%s %s", normalizePunctuation(matcher.group(1)), matcher.group(2));
		}
		return null;
	}

	public static Collection<String> reduceMovieNamePermutations(Collection<String> terms) throws IOException {
		LinkedList<String> names = new LinkedList<String>();

		for (String it : terms) {
			String rn = reduceMovieName(it, true);
			if (rn != null) {
				names.addFirst(rn);
			} else {
				names.addLast(it); // unsure, keep original term just in case, but also try non-strict reduce
				rn = reduceMovieName(it, false);
				if (rn != null) {
					names.addLast(rn);
				}
			}
		}

		return names;
	}

	public static File guessMovieFolder(File movieFile) throws Exception {
		File folder = guessMovieFolderWithoutSanity(movieFile);

		// perform sanity checks
		if (folder == null || folder.getName().isEmpty() || folder.equals(new File(System.getProperty("user.home")))) {
			return null;
		}

		return folder;
	}

	private static File guessMovieFolderWithoutSanity(File movieFile) throws Exception {
		// special case for folder mode
		if (movieFile.isDirectory()) {
			File f = movieFile;

			// check for double nested structures
			if (checkMovie(f.getParentFile(), false) != null && checkMovie(f, false) == null) {
				return f.getParentFile();
			} else {
				return f;
			}
		}

		// first parent folder that matches a movie (max 3 levels deep)
		for (boolean strictness : new boolean[] { true, false }) {
			File f = movieFile.getParentFile();
			for (int i = 0; f != null && i < 3; f = f.getParentFile(), i++) {
				String term = stripReleaseInfo(f.getName());
				if (term.length() > 0 && checkMovie(f, strictness) != null) {
					return f;
				}
			}
		}

		// otherwise try the first potentially meaningful parent folder (max 2 levels deep)
		File f = movieFile.getParentFile();
		for (int i = 0; f != null && i < 2; f = f.getParentFile(), i++) {
			String term = stripReleaseInfo(f.getName());
			if (term.length() > 0) {
				// check for double nested structures
				if (checkMovie(f.getParentFile(), false) != null && checkMovie(f, false) == null) {
					return f.getParentFile();
				} else {
					return f;
				}
			}
		}

		if (movieFile.getParentFile() != null && stripReleaseInfo(movieFile.getParentFile().getName()).length() > 0) {
			return movieFile.getParentFile();
		}
		return null;
	}

	public static Movie checkMovie(File file, boolean strict) throws Exception {
		List<Movie> matches = file != null ? matchMovieName(singleton(file.getName()), strict, 4) : null;
		return matches != null && matches.size() > 0 ? matches.get(0) : null;
	}

	private static List<Entry<String, Movie>> movieIndex = new ArrayList<Entry<String, Movie>>(100000);

	public static synchronized List<Entry<String, Movie>> getMovieIndex() throws IOException {
		if (movieIndex.isEmpty()) {
			try {
				for (Movie movie : releaseInfo.getMovieList()) {
					movieIndex.add(new SimpleEntry<String, Movie>(normalizePunctuation(movie.getName()).toLowerCase(), movie));
				}
			} catch (Exception e) {
				// can't load movie index, just try again next time
				Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.SEVERE, "Failed to load movie index: " + e.getMessage(), e);
				return emptyList();
			}
		}

		return movieIndex;
	}

	public static List<Movie> matchMovieName(final Collection<String> files, boolean strict, int maxStartIndex) throws Exception {
		// cross-reference file / folder name with movie list
		final HighPerformanceMatcher nameMatcher = new HighPerformanceMatcher(maxStartIndex);
		final Map<Movie, String> matchMap = new HashMap<Movie, String>();

		for (Entry<String, Movie> movie : getMovieIndex()) {
			for (String name : files) {
				String movieIdentifier = movie.getKey();
				String commonName = nameMatcher.matchFirstCommonSequence(name, movieIdentifier);
				if (commonName != null && commonName.length() >= movieIdentifier.length()) {
					String strictMovieIdentifier = movie.getKey() + " " + movie.getValue().getYear();
					String strictCommonName = nameMatcher.matchFirstCommonSequence(name, strictMovieIdentifier);
					if (strictCommonName != null && strictCommonName.length() >= strictMovieIdentifier.length()) {
						// prefer strict match
						matchMap.put(movie.getValue(), strictCommonName);
					} else if (!strict) {
						// make sure the common identifier is not just the year
						matchMap.put(movie.getValue(), commonName);
					}
				}
			}
		}

		// sort by length of name match (descending)
		List<Movie> results = new ArrayList<Movie>(matchMap.keySet());
		sort(results, new Comparator<Movie>() {

			@Override
			public int compare(Movie a, Movie b) {
				return Integer.valueOf(matchMap.get(b).length()).compareTo(Integer.valueOf(matchMap.get(a).length()));
			}
		});

		return results;
	}

	public static List<Movie> matchMovieFromStringWithoutSpacing(Collection<String> names, boolean strict) throws IOException {
		// clear name of punctuation, spacing, and leading 'The' or 'A' that are common causes for word-lookup to fail
		Pattern spacing = Pattern.compile("(^(?i)(The|A)\\b)|[\\p{Punct}\\p{Space}]+");

		List<String> terms = new ArrayList<String>(names.size());
		for (String it : names) {
			String term = spacing.matcher(it).replaceAll("").toLowerCase();
			if (term.length() >= 3) {
				terms.add(term); // only consider words, not just random letters
			}
		}

		// similarity threshold based on strict/non-strict
		SimilarityMetric metric = new NameSimilarityMetric();
		float similarityThreshold = strict ? 0.9f : 0.5f;

		LinkedList<Movie> movies = new LinkedList<Movie>();
		for (Entry<String, Movie> it : getMovieIndex()) {
			String name = spacing.matcher(it.getKey()).replaceAll("").toLowerCase();
			for (String term : terms) {
				if (term.contains(name)) {
					String year = String.valueOf(it.getValue().getYear());
					if (term.contains(year) && metric.getSimilarity(term, name + year) > similarityThreshold) {
						movies.addFirst(it.getValue());
					} else if (metric.getSimilarity(term, name) > similarityThreshold) {
						movies.addLast(it.getValue());
					}
					break;
				}
			}
		}

		return new ArrayList<Movie>(movies);
	}

	private static Collection<Movie> queryMovieByFileName(Collection<String> files, MovieIdentificationService queryLookupService, Locale locale) throws Exception {
		// remove blacklisted terms
		List<String> querySet = new ArrayList<String>();
		querySet.addAll(stripReleaseInfo(files, true));
		querySet.addAll(stripReleaseInfo(files, false));

		// remove duplicates
		querySet = getUniqueQuerySet(stripBlacklistedTerms(querySet));

		// DEBUG
		// System.out.format("Query %s: %s%n", queryLookupService.getName(), querySet);

		final Map<Movie, Float> probabilityMap = new LinkedHashMap<Movie, Float>();
		final SimilarityMetric metric = getMovieMatchMetric();
		for (String query : querySet) {
			for (Movie movie : queryLookupService.searchMovie(query.toLowerCase(), locale)) {
				probabilityMap.put(movie, metric.getSimilarity(query, movie));
			}
		}

		// sort by similarity to original query (descending)
		List<Movie> results = new ArrayList<Movie>(probabilityMap.keySet());
		sort(results, new Comparator<Movie>() {

			@Override
			public int compare(Movie a, Movie b) {
				return probabilityMap.get(b).compareTo(probabilityMap.get(a));
			}
		});

		return results;
	}

	private static List<String> getUniqueQuerySet(Collection<String> terms) {
		Map<String, String> unique = new LinkedHashMap<String, String>();
		for (String it : terms) {
			if (it.length() > 0) {
				unique.put(normalizePunctuation(it).toLowerCase(), it);
			}
		}
		return new ArrayList<String>(unique.values());
	}

	public static String stripReleaseInfo(String name) {
		try {
			return releaseInfo.cleanRelease(singleton(name), true).iterator().next();
		} catch (NoSuchElementException e) {
			return ""; // default value in case all tokens are stripped away
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> stripReleaseInfo(Collection<String> names, boolean strict) throws IOException {
		return releaseInfo.cleanRelease(names, strict);
	}

	public static List<String> stripBlacklistedTerms(Collection<String> names) throws IOException {
		Pattern blacklist = releaseInfo.getBlacklistPattern();
		List<String> acceptables = new ArrayList<String>(names.size());
		for (String it : names) {
			if (blacklist.matcher(it).replaceAll("").trim().length() > 0) {
				acceptables.add(it);
			}
		}
		return acceptables;
	}

	public static Set<Integer> grepImdbIdFor(File file) throws Exception {
		Set<Integer> collection = new LinkedHashSet<Integer>();
		List<File> nfoFiles = new ArrayList<File>();
		if (file.isDirectory()) {
			nfoFiles.addAll(filter(listFiles(singleton(file), 10, false), NFO_FILES));
		} else if (file.getParentFile().isDirectory()) {
			addAll(nfoFiles, file.getParentFile().listFiles(NFO_FILES));
		}

		// parse ids from nfo files
		for (File nfo : nfoFiles) {
			try {
				String text = new String(readFile(nfo), "UTF-8");
				collection.addAll(grepImdbId(text));
			} catch (Exception e) {
				Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to read nfo: " + e.getMessage());
			}
		}

		return collection;
	}

	public static Set<SearchResult> lookupSeriesNameByInfoFile(Collection<File> files, Locale language) throws Exception {
		Set<SearchResult> names = new LinkedHashSet<SearchResult>();

		SortedSet<File> folders = new TreeSet<File>(reverseOrder());
		for (File f : files) {
			for (int i = 0; i < 2 && f.getParentFile() != null; i++) {
				f = f.getParentFile();
				folders.add(f);
			}
		}

		// search for id in sibling nfo files
		for (File folder : folders) {
			if (!folder.exists())
				continue;

			for (File nfo : folder.listFiles(NFO_FILES)) {
				String text = new String(readFile(nfo), "UTF-8");

				for (int imdbid : grepImdbId(text)) {
					TheTVDBSearchResult series = WebServices.TheTVDB.lookupByIMDbID(imdbid, language);
					if (series != null) {
						names.add(series);
					}
				}

				for (int tvdbid : grepTheTvdbId(text)) {
					TheTVDBSearchResult series = WebServices.TheTVDB.lookupByID(tvdbid, language);
					if (series != null) {
						names.add(series);
					}
				}
			}
		}

		return names;
	}

	public static Set<Integer> grepImdbId(CharSequence text) {
		// scan for imdb id patterns like tt1234567
		Matcher imdbMatch = Pattern.compile("(?<=tt)\\d{7}").matcher(text);
		Set<Integer> collection = new LinkedHashSet<Integer>();

		while (imdbMatch.find()) {
			collection.add(Integer.parseInt(imdbMatch.group()));
		}

		return collection;
	}

	public static Set<Integer> grepTheTvdbId(CharSequence text) {
		// scan for thetvdb id patterns like http://www.thetvdb.com/?tab=series&id=78874&lid=14
		Set<Integer> collection = new LinkedHashSet<Integer>();
		for (String token : Pattern.compile("[\\s\"<>|]+").split(text)) {
			try {
				URL url = new URL(token);
				if (url.getHost().contains("thetvdb") && url.getQuery() != null) {
					Matcher idMatch = Pattern.compile("(?<=(^|\\W)id=)\\d+").matcher(url.getQuery());
					while (idMatch.find()) {
						collection.add(Integer.parseInt(idMatch.group()));
					}
				}
			} catch (MalformedURLException e) {
				// parse for thetvdb urls, ignore everything else
			}
		}

		return collection;
	}

	public static Movie grepMovie(File nfo, MovieIdentificationService resolver, Locale locale) throws Exception {
		return resolver.getMovieDescriptor(grepImdbId(new String(readFile(nfo), "UTF-8")).iterator().next(), locale);
	}

	public static SeriesInfo grepSeries(File nfo, Locale locale) throws Exception {
		return WebServices.TheTVDB.getSeriesInfoByID(grepTheTvdbId(new String(readFile(nfo), "UTF-8")).iterator().next(), locale);
	}

	public static Movie tmdb2imdb(Movie m) throws IOException {
		if (m.getTmdbId() <= 0 && m.getImdbId() <= 0)
			throw new IllegalArgumentException();

		MovieInfo info = WebServices.TMDb.getMovieInfo(m, Locale.ENGLISH);
		return new Movie(info.getName(), info.getReleased().getYear(), info.getImdbId(), info.getId());
	}

	/*
	 * Heavy-duty name matcher used for matching a file to or more movies (out of a list of ~50k)
	 */
	private static class HighPerformanceMatcher extends CommonSequenceMatcher {

		private static final Collator collator = getLenientCollator(Locale.ENGLISH);

		private static final Map<String, CollationKey[]> transformCache = synchronizedMap(new HashMap<String, CollationKey[]>(65536));

		public HighPerformanceMatcher(int maxStartIndex) {
			super(collator, maxStartIndex, true);
		}

		@Override
		protected CollationKey[] split(String sequence) {
			CollationKey[] value = transformCache.get(sequence);
			if (value == null) {
				value = super.split(normalize(sequence));
				transformCache.put(sequence, value);
			}
			return value;
		}

		public String normalize(String sequence) {
			return normalizePunctuation(sequence); // only normalize punctuation, make sure we keep the year (important for movie matching)
		}
	}

	public static void storeMetaInfo(File file, Object model) {
		// only for Episode / Movie objects
		if ((model instanceof Episode || model instanceof Movie) && file.exists()) {
			MetaAttributes xattr = new MetaAttributes(file);

			// set creation date to episode / movie release date
			try {
				if (model instanceof Episode) {
					Episode episode = (Episode) model;
					if (episode.getAirdate() != null) {
						xattr.setCreationDate(episode.getAirdate().getTimeStamp());
					}
				} else if (model instanceof Movie) {
					Movie movie = (Movie) model;
					if (movie.getYear() > 0) {
						xattr.setCreationDate(new Date(movie.getYear(), 1, 1).getTimeStamp());
					}
				}
			} catch (Exception e) {
				Logger.getLogger(MediaDetection.class.getClass().getName()).warning("Failed to set creation date: " + e.getMessage());
			}

			// store original name and model as xattr
			try {
				if (xattr.getOriginalName() == null) {
					xattr.setOriginalName(file.getName());
				}
				xattr.setObject(model);
			} catch (Exception e) {
				Logger.getLogger(MediaDetection.class.getClass().getName()).warning("Failed to set xattr: " + e.getMessage());
			}

		}
	}
}
