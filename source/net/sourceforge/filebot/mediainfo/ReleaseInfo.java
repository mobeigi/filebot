
package net.sourceforge.filebot.mediainfo;


import static java.util.ResourceBundle.*;
import static java.util.concurrent.TimeUnit.*;
import static java.util.regex.Pattern.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.web.CachedResource;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TheTVDBClient.TheTVDBSearchResult;


public class ReleaseInfo {
	
	public static List<String> detectSeriesNames(Collection<File> files) throws Exception {
		ReleaseInfo releaseInfo = new ReleaseInfo();
		
		// don't allow duplicates
		Map<String, String> names = new LinkedHashMap<String, String>();
		
		try {
			for (SearchResult it : releaseInfo.lookupSeriesNameByInfoFile(files, Locale.ENGLISH)) {
				names.put(it.getName().toLowerCase(), it.getName());
			}
		} catch (Exception e) {
			Logger.getLogger(ReleaseInfo.class.getClass().getName()).log(Level.WARNING, "Failed to lookup info by id: " + e.getMessage());
		}
		
		// match common word sequence and clean detected word sequence from unwanted elements
		Collection<String> matches = new SeriesNameMatcher().matchAll(files.toArray(new File[files.size()]));
		for (String it : releaseInfo.cleanRG(matches)) {
			names.put(it.toLowerCase(), it);
		}
		
		return new ArrayList<String>(names.values());
	}
	
	
	public static Set<Integer> grepImdbIdFor(File file) throws Exception {
		ReleaseInfo releaseInfo = new ReleaseInfo();
		Set<Integer> collection = new LinkedHashSet<Integer>();
		
		for (File nfo : file.getParentFile().listFiles(MediaTypes.getDefaultFilter("application/nfo"))) {
			String text = new String(readFile(nfo), "UTF-8");
			collection.addAll(releaseInfo.grepImdbId(text));
		}
		
		return collection;
	}
	
	
	public Set<SearchResult> lookupSeriesNameByInfoFile(Collection<File> files, Locale language) throws Exception {
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
	
	
	public Set<Integer> grepImdbId(CharSequence text) {
		// scan for imdb id patterns like tt1234567
		Matcher imdbMatch = Pattern.compile("(?<=tt)\\d{7}").matcher(text);
		Set<Integer> collection = new LinkedHashSet<Integer>();
		
		while (imdbMatch.find()) {
			collection.add(Integer.parseInt(imdbMatch.group()));
		}
		
		return collection;
	}
	
	
	public Set<Integer> grepTheTvdbId(CharSequence text) {
		// scan for thetvdb id patterns like http://www.thetvdb.com/?tab=series&id=78874&lid=14
		Set<Integer> collection = new LinkedHashSet<Integer>();
		for (String token : Pattern.compile("[\\s\"<>|]+").split(text)) {
			try {
				URL url = new URL(token);
				if (url.getHost().contains("thetvdb")) {
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
	
	
	public String getVideoSource(File file) {
		// check parent and itself for group names
		return matchLast(getVideoSourcePattern(), file.getParent(), file.getName());
	}
	
	
	public String getReleaseGroup(File file) throws IOException {
		// check parent and itself for group names
		return matchLast(getReleaseGroupPattern(), file.getParent(), file.getName());
	}
	
	
	protected String matchLast(Pattern pattern, CharSequence... sequence) {
		String lastMatch = null;
		
		for (CharSequence name : sequence) {
			if (name == null)
				continue;
			
			Matcher matcher = pattern.matcher(name);
			while (matcher.find()) {
				lastMatch = matcher.group();
			}
		}
		
		return lastMatch;
	}
	
	
	public List<String> clean(Iterable<String> items) throws IOException {
		return clean(items, getVideoSourcePattern(), getCodecPattern());
	}
	
	
	public String clean(String item) throws IOException {
		return clean(item, getVideoSourcePattern(), getCodecPattern());
	}
	
	
	public List<String> cleanRG(Iterable<String> items) throws IOException {
		return clean(items, getReleaseGroupPattern(), getVideoSourcePattern(), getCodecPattern());
	}
	
	
	public String cleanRG(String item) throws IOException {
		return clean(item, getReleaseGroupPattern(), getVideoSourcePattern(), getCodecPattern());
	}
	
	
	public List<String> clean(Iterable<String> items, Pattern... blacklisted) {
		List<String> cleanedItems = new ArrayList<String>();
		for (String it : items) {
			cleanedItems.add(clean(it, blacklisted));
		}
		
		return cleanedItems;
	}
	
	
	public String clean(String item, Pattern... blacklisted) {
		for (Pattern it : blacklisted) {
			item = it.matcher(item).replaceAll("");
		}
		
		return item.replaceAll("[\\p{Punct}\\p{Space}]+", " ").trim();
	}
	
	
	public Pattern getCodecPattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.codec");
		return compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	
	
	public Pattern getVideoSourcePattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.video.source");
		return compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	
	
	public Pattern getReleaseGroupPattern() throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(releaseGroupResource.get(), "|") + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	
	
	// fetch release group names online and try to update the data every other day
	protected final CachedResource<String[]> releaseGroupResource = new CachedResource<String[]>(getBundle(getClass().getName()).getString("url.release-groups"), DAYS.toMillis(2)) {
		
		@Override
		public String[] process(ByteBuffer data) {
			return compile("\\s+").split(Charset.forName("UTF-8").decode(data));
		}
	};
	
}
