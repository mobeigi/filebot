
package net.sourceforge.filebot.media;


import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.ResourceBundle.*;
import static java.util.regex.Pattern.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.Collator;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import net.sourceforge.filebot.web.CachedResource;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.tuned.ByteBufferInputStream;


public class ReleaseInfo {
	
	public String getVideoSource(File file) {
		// check parent and itself for group names
		return matchLast(getVideoSourcePattern(), getBundle(getClass().getName()).getString("pattern.video.source").split("[|]"), file.getParent(), file.getName());
	}
	
	
	public String getReleaseGroup(File file) throws IOException {
		// check file and folder for release group names
		String[] groups = releaseGroupResource.get();
		String[] files = new String[] { file.getParentFile().getName(), file.getName() };
		
		// try case-sensitive match
		String match = matchLast(getReleaseGroupPattern(true), groups, files);
		
		// try case-insensitive match as fallback
		if (match == null) {
			match = matchLast(getReleaseGroupPattern(false), groups, files);
		}
		
		return match;
	}
	
	
	public Locale getLanguageSuffix(String name) {
		// match locale identifier and lookup Locale object
		Map<String, Locale> languages = getLanguageMap(Locale.ENGLISH, Locale.getDefault());
		
		String lang = matchLast(getLanguageSuffixPattern(languages.keySet()), null, name);
		if (lang == null)
			return null;
		
		return languages.get(lang);
	}
	
	
	protected String matchLast(Pattern pattern, String[] standardValues, CharSequence... sequence) {
		String lastMatch = null;
		
		// match last occurrence
		for (CharSequence name : sequence) {
			if (name == null)
				continue;
			
			Matcher matcher = pattern.matcher(name);
			while (matcher.find()) {
				lastMatch = matcher.group();
			}
		}
		
		// prefer standard value over matched value
		if (lastMatch != null && standardValues != null) {
			for (String standard : standardValues) {
				if (standard.equalsIgnoreCase(lastMatch)) {
					return standard;
				}
			}
		}
		
		return lastMatch;
	}
	
	
	public List<String> cleanRelease(Collection<String> items, boolean strict) throws IOException {
		Set<String> languages = getLanguageMap(Locale.ENGLISH, Locale.getDefault()).keySet();
		
		Pattern clutterBracket = getClutterBracketPattern(strict);
		Pattern releaseGroup = getReleaseGroupPattern(strict);
		Pattern languageSuffix = getLanguageSuffixPattern(languages);
		Pattern languageTag = getLanguageTagPattern(languages);
		Pattern videoSource = getVideoSourcePattern();
		Pattern videoFormat = getVideoFormatPattern();
		Pattern resolution = getResolutionPattern();
		Pattern queryBlacklist = getBlacklistPattern();
		
		Pattern[] stopwords = new Pattern[] { getReleaseGroupPattern(true), languageTag, videoSource, videoFormat, resolution, languageSuffix };
		Pattern[] blacklist = new Pattern[] { clutterBracket, releaseGroup, languageTag, videoSource, videoFormat, resolution, languageSuffix, queryBlacklist };
		
		List<String> output = new ArrayList<String>(items.size());
		for (String it : items) {
			it = substringBefore(it, stopwords);
			it = clean(it, blacklist);
			
			// ignore empty values
			if (it.length() > 0) {
				output.add(it);
			}
		}
		
		return output;
	}
	
	
	public String clean(String item, Pattern... blacklisted) {
		for (Pattern it : blacklisted) {
			item = it.matcher(item).replaceAll("");
		}
		
		return normalizePunctuation(item);
	}
	
	
	public String substringBefore(String item, Pattern... stopwords) {
		for (Pattern it : stopwords) {
			Matcher matcher = it.matcher(item);
			if (matcher.find()) {
				item = item.substring(0, matcher.start()); // use substring before the matched stopword
			}
		}
		return item;
	}
	
	
	public Pattern getLanguageTagPattern(Collection<String> languages) {
		// [en]
		return compile("(?<=[-\\[{(])(" + join(quoteAll(languages), "|") + ")(?=\\p{Punct})", CASE_INSENSITIVE | UNICODE_CASE | CANON_EQ);
	}
	
	
	public Pattern getLanguageSuffixPattern(Collection<String> languages) {
		// .en.srt
		return compile("(?<=[\\p{Punct}\\p{Space}])(" + join(quoteAll(languages), "|") + ")(?=[._ ]*$)", CASE_INSENSITIVE | UNICODE_CASE | CANON_EQ);
	}
	
	
	public Pattern getResolutionPattern() {
		// match screen resolutions 640x480, 1280x720, etc
		return compile("(?<!\\p{Alnum})(\\d{4}|[6-9]\\d{2})x(\\d{4}|[4-9]\\d{2})(?!\\p{Alnum})");
	}
	
	
	public Pattern getVideoFormatPattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.video.format");
		return compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	
	
	public Pattern getVideoSourcePattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.video.source");
		return compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	
	
	public Pattern getClutterBracketPattern(boolean strict) {
		// match patterns like [Action, Drama] or {ENG-XViD-MP3-DVDRiP} etc
		String contentFilter = strict ? "[\\p{Space}\\p{Punct}&&[^\\[\\]]]" : "\\p{Alpha}";
		return compile("(?:\\[([^\\[\\]]+?" + contentFilter + "[^\\[\\]]+?)\\])|(?:\\{([^\\{\\}]+?" + contentFilter + "[^\\{\\}]+?)\\})|(?:\\(([^\\(\\)]+?" + contentFilter + "[^\\(\\)]+?)\\))");
	}
	
	
	public synchronized Pattern getReleaseGroupPattern(boolean strict) throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(releaseGroupResource.get(), "|") + ")(?!\\p{Alnum})", strict ? 0 : CASE_INSENSITIVE | UNICODE_CASE | CANON_EQ);
	}
	
	
	public synchronized Pattern getBlacklistPattern() throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(queryBlacklistResource.get(), "|") + ")(?!\\p{Alnum})", CASE_INSENSITIVE | UNICODE_CASE | CANON_EQ);
	}
	
	
	public synchronized Movie[] getMovieList() throws IOException {
		return movieListResource.get();
	}
	
	
	public synchronized String[] getSeriesList() throws IOException {
		return seriesListResource.get();
	}
	
	
	public FileFilter getDiskFolderFilter() {
		return new FolderEntryFilter(compile(getBundle(getClass().getName()).getString("pattern.diskfolder.entry")));
	}
	
	
	public FileFilter getClutterFileFilter() {
		return new FileFolderNameFilter(compile(getBundle(getClass().getName()).getString("pattern.file.ignore")));
	}
	
	
	// fetch release group names online and try to update the data every other day
	protected final CachedResource<String[]> releaseGroupResource = new PatternResource(getBundle(getClass().getName()).getString("url.release-groups"));
	protected final CachedResource<String[]> queryBlacklistResource = new PatternResource(getBundle(getClass().getName()).getString("url.query-blacklist"));
	protected final CachedResource<Movie[]> movieListResource = new MovieResource(getBundle(getClass().getName()).getString("url.movie-list"));
	protected final CachedResource<String[]> seriesListResource = new SeriesResource(getBundle(getClass().getName()).getString("url.series-list"));
	
	
	protected static class PatternResource extends CachedResource<String[]> {
		
		public PatternResource(String resource) {
			super(resource, String[].class, 24 * 60 * 60 * 1000); // 24h update interval
		}
		
		
		@Override
		public String[] process(ByteBuffer data) {
			return compile("\\n").split(Charset.forName("UTF-8").decode(data));
		}
	}
	
	
	protected static class MovieResource extends CachedResource<Movie[]> {
		
		public MovieResource(String resource) {
			super(resource, Movie[].class, 7 * 24 * 60 * 60 * 1000); // check for updates once a week
		}
		
		
		@Override
		public Movie[] process(ByteBuffer data) throws IOException {
			Scanner scanner = new Scanner(new GZIPInputStream(new ByteBufferInputStream(data)), "UTF-8").useDelimiter("\t|\n");
			
			List<Movie> movies = new ArrayList<Movie>();
			while (scanner.hasNext()) {
				int imdbid = scanner.nextInt();
				String name = scanner.next();
				int year = scanner.nextInt();
				movies.add(new Movie(name, year, imdbid));
			}
			
			return movies.toArray(new Movie[0]);
		}
	}
	
	
	protected static class SeriesResource extends CachedResource<String[]> {
		
		public SeriesResource(String resource) {
			super(resource, String[].class, 7 * 24 * 60 * 60 * 1000); // check for updates once a week
		}
		
		
		@Override
		public String[] process(ByteBuffer data) throws IOException {
			return readAll(new InputStreamReader(new GZIPInputStream(new ByteBufferInputStream(data)), "utf-8")).split("\\n");
		}
	}
	
	
	protected static class FolderEntryFilter implements FileFilter {
		
		private final Pattern entryPattern;
		
		
		public FolderEntryFilter(Pattern entryPattern) {
			this.entryPattern = entryPattern;
		}
		
		
		@Override
		public boolean accept(File dir) {
			if (dir.isDirectory()) {
				for (String entry : dir.list()) {
					if (entryPattern.matcher(entry).matches()) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
	
	public static class FileFolderNameFilter implements FileFilter {
		
		private final Pattern namePattern;
		
		
		public FileFolderNameFilter(Pattern namePattern) {
			this.namePattern = namePattern;
		}
		
		
		@Override
		public boolean accept(File file) {
			return (namePattern.matcher(file.getName()).find() || (file.isFile() && namePattern.matcher(file.getParentFile().getName()).find()));
		}
	}
	
	
	private Collection<String> quoteAll(Collection<String> strings) {
		List<String> patterns = new ArrayList<String>(strings.size());
		for (String it : strings) {
			patterns.add(Pattern.quote(it));
		}
		return patterns;
	}
	
	
	private final Map<Set<Locale>, Map<String, Locale>> languageMapCache = synchronizedMap(new WeakHashMap<Set<Locale>, Map<String, Locale>>(2));
	
	
	private Map<String, Locale> getLanguageMap(Locale... supportedDisplayLocale) {
		// try cache
		Set<Locale> displayLocales = new HashSet<Locale>(asList(supportedDisplayLocale));
		Map<String, Locale> languageMap = languageMapCache.get(displayLocales);
		if (languageMap != null) {
			return languageMap;
		}
		
		// use maximum strength collator by default
		Collator collator = Collator.getInstance(Locale.ROOT);
		collator.setDecomposition(Collator.FULL_DECOMPOSITION);
		collator.setStrength(Collator.PRIMARY);
		
		@SuppressWarnings("unchecked")
		Comparator<String> order = (Comparator) collator;
		languageMap = new TreeMap<String, Locale>(order);
		
		for (String code : Locale.getISOLanguages()) {
			Locale locale = new Locale(code);
			languageMap.put(locale.getLanguage(), locale);
			languageMap.put(locale.getISO3Language(), locale);
			
			// map display language names for given locales
			for (Locale language : displayLocales) {
				// make sure language name is properly normalized so accents and whatever don't break the regex pattern syntax
				String languageName = Normalizer.normalize(locale.getDisplayLanguage(language), Form.NFKD);
				languageMap.put(languageName, locale);
			}
		}
		
		// remove illegal tokens
		languageMap.remove("");
		languageMap.remove("II");
		languageMap.remove("III");
		
		Map<String, Locale> result = unmodifiableMap(languageMap);
		languageMapCache.put(displayLocales, result);
		return result;
	}
}
