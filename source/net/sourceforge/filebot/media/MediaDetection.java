
package net.sourceforge.filebot.media;


import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityComparator;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.web.AnidbClient.AnidbSearchResult;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TheTVDBClient.TheTVDBSearchResult;


public class MediaDetection {
	
	private static final ReleaseInfo releaseInfo = new ReleaseInfo();
	
	
	public static boolean isDiskFolder(File folder) {
		return releaseInfo.getDiskFolderFilter().accept(folder);
	}
	
	
	public static Map<Set<File>, Set<String>> mapSeriesNamesByFiles(Collection<File> files, Locale locale) throws Exception {
		SortedMap<File, List<File>> filesByFolder = mapByFolder(filter(files, VIDEO_FILES, SUBTITLE_FILES));
		
		// map series names by folder
		Map<File, Set<String>> seriesNamesByFolder = new HashMap<File, Set<String>>();
		
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
			batchSets.put(combinedFileSet, combinedNameSet);
			
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
	
	
	public static List<String> detectSeriesNames(Collection<File> files, Locale locale) throws Exception {
		// don't allow duplicates
		Map<String, String> names = new LinkedHashMap<String, String>();
		
		try {
			for (SearchResult it : lookupSeriesNameByInfoFile(files, locale)) {
				names.put(it.getName().toLowerCase(), it.getName());
			}
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to lookup info by id: " + e.getMessage(), e);
		}
		
		// cross-reference known series names against file structure
		try {
			Set<String> folders = new LinkedHashSet<String>();
			for (File f : files) {
				for (int i = 0; i < 3 && f != null; i++, f = f.getParentFile()) {
					if (i != 0) {
						folders.add(f.getName());
					}
				}
			}
			
			// match know name from filename if there is not enough context for CWS matching
			if (files.size() == 1) {
				folders.add(files.iterator().next().getName());
			}
			
			// match folder names against known series names
			for (TheTVDBSearchResult match : matchSeriesByName(folders.toArray(new String[0]))) {
				names.put(match.getName().toLowerCase(), match.getName());
			}
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to match folder structure: " + e.getMessage(), e);
		}
		
		// match common word sequence and clean detected word sequence from unwanted elements
		SeriesNameMatcher matcher = new SeriesNameMatcher(getLenientCollator(locale));
		Collection<String> matches = matcher.matchAll(files.toArray(new File[files.size()]));
		try {
			matches = stripReleaseInfo(matches, true);
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to clean matches: " + e.getMessage(), e);
		}
		for (String it : matches) {
			names.put(it.toLowerCase(), it);
		}
		
		return new ArrayList<String>(names.values());
	}
	
	
	public static Collection<TheTVDBSearchResult> matchSeriesByName(String... names) throws Exception {
		final HighPerformanceMatcher nameMatcher = new HighPerformanceMatcher(0);
		final Map<TheTVDBSearchResult, String> matchMap = new HashMap<TheTVDBSearchResult, String>();
		
		for (final TheTVDBSearchResult entry : releaseInfo.getSeriesList()) {
			for (String name : names) {
				String identifier = nameMatcher.normalize(entry.getName());
				String commonName = nameMatcher.matchByFirstCommonWordSequence(name, identifier);
				if (commonName != null && commonName.length() >= identifier.length()) {
					matchMap.put(entry, commonName);
				}
			}
		}
		
		// sort by length of name match (descending)
		List<TheTVDBSearchResult> results = new ArrayList<TheTVDBSearchResult>(matchMap.keySet());
		sort(results, new Comparator<TheTVDBSearchResult>() {
			
			@Override
			public int compare(TheTVDBSearchResult a, TheTVDBSearchResult b) {
				return Integer.compare(matchMap.get(b).length(), matchMap.get(a).length());
			}
		});
		
		return results;
	}
	
	
	public static Collection<AnidbSearchResult> matchAnimeByName(String... names) throws Exception {
		final HighPerformanceMatcher nameMatcher = new HighPerformanceMatcher(0);
		final Map<AnidbSearchResult, String> matchMap = new HashMap<AnidbSearchResult, String>();
		
		for (final AnidbSearchResult entry : WebServices.AniDB.getAnimeTitles()) {
			for (String name : names) {
				for (String identifier : new String[] { entry.getPrimaryTitle(), entry.getOfficialTitle("en") }) {
					if (identifier == null || identifier.isEmpty())
						continue;
					
					identifier = nameMatcher.normalize(entry.getName());
					String commonName = nameMatcher.matchByFirstCommonWordSequence(name, identifier);
					if (commonName != null && commonName.length() >= identifier.length()) {
						matchMap.put(entry, commonName);
					}
				}
			}
		}
		
		// sort by length of name match (descending)
		List<AnidbSearchResult> results = new ArrayList<AnidbSearchResult>(matchMap.keySet());
		sort(results, new Comparator<AnidbSearchResult>() {
			
			@Override
			public int compare(AnidbSearchResult a, AnidbSearchResult b) {
				return Integer.compare(matchMap.get(b).length(), matchMap.get(a).length());
			}
		});
		
		return results;
	}
	
	
	public static Collection<Movie> detectMovie(File movieFile, MovieIdentificationService hashLookupService, MovieIdentificationService queryLookupService, Locale locale, boolean strict) throws Exception {
		Set<Movie> options = new LinkedHashSet<Movie>();
		
		// lookup by file hash
		if (hashLookupService != null && movieFile.isFile()) {
			for (Movie movie : hashLookupService.getMovieDescriptors(singleton(movieFile), locale).values()) {
				if (movie != null) {
					options.add(movie);
				}
			}
		}
		
		// lookup by id from nfo file
		if (queryLookupService != null) {
			// try to grep imdb id from nfo files
			for (int imdbid : grepImdbIdFor(movieFile)) {
				Movie movie = queryLookupService.getMovieDescriptor(imdbid, locale);
				if (movie != null) {
					options.add(movie);
				}
			}
		}
		
		// search by file name or folder name
		List<String> files = new ArrayList<String>();
		files.add(getName(movieFile));
		files.add(getName(movieFile.getParentFile()));
		
		List<Movie> movieNameMatches = matchMovieName(files, locale, strict);
		
		// skip further queries if collected matches are already sufficient
		if (options.size() > 0 && movieNameMatches.size() > 0) {
			options.addAll(movieNameMatches);
			return options;
		}
		
		// add local matching after online search
		options.addAll(movieNameMatches);
		
		// query by file / folder name
		if (queryLookupService != null && !strict) {
			options.addAll(queryMovieByFileName(files, queryLookupService, locale));
		}
		
		// sort by relevance
		List<Movie> optionsByRelevance = new ArrayList<Movie>(options);
		sort(optionsByRelevance, new SimilarityComparator(stripReleaseInfo(getName(movieFile))));
		return optionsByRelevance;
	}
	
	
	private static List<Movie> matchMovieName(final List<String> files, final Locale locale, final boolean strict) throws Exception {
		// cross-reference file / folder name with movie list
		final HighPerformanceMatcher nameMatcher = new HighPerformanceMatcher(3);
		final Map<Movie, String> matchMap = new HashMap<Movie, String>();
		
		for (final Movie movie : releaseInfo.getMovieList()) {
			for (String name : files) {
				String movieIdentifier = movie.getName();
				String commonName = nameMatcher.matchByFirstCommonWordSequence(name, movieIdentifier);
				if (commonName != null && commonName.length() >= movieIdentifier.length()) {
					String strictMovieIdentifier = movie.getName() + " " + movie.getYear();
					String strictCommonName = nameMatcher.matchByFirstCommonWordSequence(name, strictMovieIdentifier);
					if (strictCommonName != null && strictCommonName.length() >= strictMovieIdentifier.length()) {
						// prefer strict match
						matchMap.put(movie, strictCommonName);
					} else if (!strict) {
						// make sure the common identifier is not just the year
						matchMap.put(movie, commonName);
					}
				}
			}
		}
		
		// sort by length of name match (descending)
		List<Movie> results = new ArrayList<Movie>(matchMap.keySet());
		sort(results, new Comparator<Movie>() {
			
			@Override
			public int compare(Movie a, Movie b) {
				return Integer.compare(matchMap.get(b).length(), matchMap.get(a).length());
			}
		});
		
		return results;
	}
	
	
	private static Collection<Movie> queryMovieByFileName(List<String> files, MovieIdentificationService queryLookupService, Locale locale) throws Exception {
		// remove blacklisted terms
		Set<String> querySet = new LinkedHashSet<String>();
		querySet.addAll(stripReleaseInfo(files, true));
		querySet.addAll(stripReleaseInfo(files, false));
		
		final SimilarityMetric metric = new NameSimilarityMetric();
		final Map<Movie, Float> probabilityMap = new LinkedHashMap<Movie, Float>();
		for (String query : querySet) {
			for (Movie movie : queryLookupService.searchMovie(query, locale)) {
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
	
	
	public static String stripReleaseInfo(String name) throws IOException {
		return releaseInfo.cleanRelease(name, true);
	}
	
	
	public static List<String> stripReleaseInfo(Collection<String> names, boolean strict) throws IOException {
		return releaseInfo.cleanRelease(names, strict);
	}
	
	
	public static Set<Integer> grepImdbIdFor(File file) throws Exception {
		Set<Integer> collection = new LinkedHashSet<Integer>();
		
		for (File nfo : file.getParentFile().listFiles(MediaTypes.getDefaultFilter("application/nfo"))) {
			String text = new String(readFile(nfo), "UTF-8");
			collection.addAll(grepImdbId(text));
		}
		
		return collection;
	}
	
	
	public static Set<SearchResult> lookupSeriesNameByInfoFile(Collection<File> files, Locale language) throws Exception {
		Set<SearchResult> names = new LinkedHashSet<SearchResult>();
		
		// search for id in sibling nfo files
		for (File folder : mapByFolder(files).keySet()) {
			for (File nfo : folder.listFiles(MediaTypes.getDefaultFilter("application/nfo"))) {
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
	
	
	@SuppressWarnings("unchecked")
	public static Comparator<String> getLenientCollator(Locale locale) {
		// use maximum strength collator by default
		final Collator collator = Collator.getInstance(locale);
		collator.setDecomposition(Collator.FULL_DECOMPOSITION);
		collator.setStrength(Collator.TERTIARY);
		
		return (Comparator) collator;
	}
	
	
	/*
	 * Heavy-duty name matcher used for matching a file to or more movies (out of a list of ~50k)
	 */
	private static class HighPerformanceMatcher extends SeriesNameMatcher {
		
		private static final Map<String, String> transformCache = synchronizedMap(new WeakHashMap<String, String>(65536));
		
		
		public HighPerformanceMatcher(int commonWordSequenceMaxStartIndex) {
			super(String.CASE_INSENSITIVE_ORDER, commonWordSequenceMaxStartIndex); // 3-4x faster than a Collator 
		}
		
		
		@Override
		protected String normalize(String source) {
			String value = transformCache.get(source);
			if (value == null) {
				value = normalizePunctuation(source); // only normalize punctuation, make sure we keep the year (important for movie matching)
				transformCache.put(source, value);
			}
			return transformCache.get(source);
		}
	}
	
}
