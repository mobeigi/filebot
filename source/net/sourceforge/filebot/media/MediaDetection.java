
package net.sourceforge.filebot.media;


import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TheTVDBClient.TheTVDBSearchResult;


public class MediaDetection {
	
	public static Map<Set<File>, Set<String>> mapSeriesNamesByFiles(Collection<File> files) throws Exception {
		SortedMap<File, List<File>> filesByFolder = mapByFolder(filter(files, VIDEO_FILES, SUBTITLE_FILES));
		
		// map series names by folder
		Map<File, Set<String>> seriesNamesByFolder = new HashMap<File, Set<String>>();
		
		for (Entry<File, List<File>> it : filesByFolder.entrySet()) {
			Set<String> namesForFolder = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			namesForFolder.addAll(detectSeriesNames(it.getValue()));
			
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
			Set<String> combinedNameSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
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
	
	
	public static List<String> detectSeriesNames(Collection<File> files) throws Exception {
		// don't allow duplicates
		Map<String, String> names = new LinkedHashMap<String, String>();
		
		try {
			for (SearchResult it : lookupSeriesNameByInfoFile(files, Locale.ENGLISH)) {
				names.put(it.getName().toLowerCase(), it.getName());
			}
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to lookup info by id: " + e.getMessage(), e);
		}
		
		// match common word sequence and clean detected word sequence from unwanted elements
		Collection<String> matches = new SeriesNameMatcher().matchAll(files.toArray(new File[files.size()]));
		
		try {
			matches = stripReleaseInfo(matches);
		} catch (Exception e) {
			Logger.getLogger(MediaDetection.class.getClass().getName()).log(Level.WARNING, "Failed to clean matches: " + e.getMessage(), e);
		}
		
		for (String it : matches) {
			names.put(it.toLowerCase(), it);
		}
		
		return new ArrayList<String>(names.values());
	}
	
	
	public static Collection<Movie> detectMovie(File movieFile, MovieIdentificationService service, Locale locale, boolean strict) throws Exception {
		Set<Movie> options = new LinkedHashSet<Movie>();
		
		// try to grep imdb id from nfo files
		for (int imdbid : grepImdbIdFor(movieFile)) {
			Movie movie = service.getMovieDescriptor(imdbid, locale);
			
			if (movie != null) {
				options.add(movie);
			}
		}
		
		if (!strict && options.isEmpty()) {
			// search by file name or folder name
			Collection<String> searchQueries = new LinkedHashSet<String>();
			searchQueries.add(getName(movieFile));
			searchQueries.add(getName(movieFile.getParentFile()));
			
			// remove blacklisted terms
			searchQueries = stripReleaseInfo(searchQueries);
			
			final SimilarityMetric metric = new NameSimilarityMetric();
			final Map<Movie, Float> probabilityMap = new LinkedHashMap<Movie, Float>();
			for (String query : searchQueries) {
				for (Movie movie : service.searchMovie(query, locale)) {
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
			
			options.addAll(results);
		}
		
		return options;
	}
	
	
	public static String stripReleaseInfo(String name) throws IOException {
		return new ReleaseInfo().cleanRelease(name);
	}
	
	
	public static List<String> stripReleaseInfo(Collection<String> names) throws IOException {
		return new ReleaseInfo().cleanRelease(names);
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
	
}
