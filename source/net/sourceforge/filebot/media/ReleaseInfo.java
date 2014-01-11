package net.sourceforge.filebot.media;

import static java.lang.Integer.*;
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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.Collator;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.web.AnidbSearchResult;
import net.sourceforge.filebot.web.CachedResource;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.TheTVDBSearchResult;
import net.sourceforge.tuned.ByteBufferInputStream;
import net.sourceforge.tuned.FileUtilities.RegexFileFilter;

import org.tukaani.xz.XZInputStream;

public class ReleaseInfo {

	public String getVideoSource(String... strings) {
		// check parent and itself for group names
		return matchLast(getVideoSourcePattern(), getBundle(getClass().getName()).getString("pattern.video.source").split("[|]"), strings);
	}

	public String getReleaseGroup(String... strings) throws IOException {
		// check file and folder for release group names
		String[] groups = releaseGroupResource.get();

		// try case-sensitive match
		String match = matchLast(getReleaseGroupPattern(true), groups, strings);

		// try case-insensitive match as fallback
		if (match == null) {
			match = matchLast(getReleaseGroupPattern(false), groups, strings);
		}

		return match;
	}

	public Locale getLanguageSuffix(String name) {
		// match locale identifier and lookup Locale object
		Map<String, Locale> languages = getLanguageMap(Locale.ENGLISH, Locale.getDefault());

		String lang = matchLast(getLanguageSuffixPattern(languages.keySet(), false), null, name);
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

	// cached patterns
	private final Map<Boolean, Pattern[]> stopwords = new HashMap<Boolean, Pattern[]>(2);
	private final Map<Boolean, Pattern[]> blacklist = new HashMap<Boolean, Pattern[]>(2);

	public List<String> cleanRelease(Collection<String> items, boolean strict) throws IOException {
		Pattern[] stopwords;
		Pattern[] blacklist;

		// initialize cached patterns
		synchronized (this.stopwords) {
			stopwords = this.stopwords.get(strict);
			blacklist = this.blacklist.get(strict);

			if (stopwords == null || blacklist == null) {
				Set<String> languages = getLanguageMap(Locale.ENGLISH, Locale.getDefault()).keySet();
				Pattern clutterBracket = getClutterBracketPattern(strict);
				Pattern releaseGroup = getReleaseGroupPattern(strict);
				Pattern languageSuffix = getLanguageSuffixPattern(languages, strict);
				Pattern languageTag = getLanguageTagPattern(languages);
				Pattern videoSource = getVideoSourcePattern();
				Pattern videoFormat = getVideoFormatPattern(strict);
				Pattern resolution = getResolutionPattern();
				Pattern queryBlacklist = getBlacklistPattern();

				stopwords = new Pattern[] { languageTag, videoSource, videoFormat, resolution, languageSuffix };
				blacklist = new Pattern[] { queryBlacklist, languageTag, clutterBracket, releaseGroup, videoSource, videoFormat, resolution, languageSuffix };

				// cache compiled patterns for common usage
				this.stopwords.put(strict, stopwords);
				this.blacklist.put(strict, blacklist);
			}
		}

		List<String> output = new ArrayList<String>(items.size());
		for (String it : items) {
			it = strict ? clean(it, stopwords) : substringBefore(it, stopwords);
			it = normalizePunctuation(clean(it, blacklist));

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
		return item;
	}

	public String substringBefore(String item, Pattern... stopwords) {
		for (Pattern it : stopwords) {
			Matcher matcher = it.matcher(item);
			if (matcher.find()) {
				String substring = item.substring(0, matcher.start()); // use substring before the matched stopword
				if (normalizePunctuation(substring).length() >= 3) {
					item = substring; // make sure that the substring has enough data
				}
			}
		}
		return item;
	}

	// cached patterns
	private Set<File> volumeRoots;
	private Pattern structureRootFolderPattern;

	public Set<File> getVolumeRoots() {
		if (volumeRoots == null) {
			Set<File> volumes = new HashSet<File>();

			// user root folder
			volumes.add(new File(System.getProperty("user.home")));

			// Windows / Linux / Mac system roots
			addAll(volumes, File.listRoots());

			if (File.separator.equals("/")) {
				// Linux and Mac system root folders
				for (File root : File.listRoots()) {
					File[] f = root.listFiles(FOLDERS);
					if (f != null) {
						addAll(volumes, f);
					}
				}

				// user-specific media roots
				for (File root : getMediaRoots()) {
					if (root.isDirectory()) {
						File[] f = root.listFiles(FOLDERS);
						if (f != null) {
							addAll(volumes, f);
						}
					}
				}
			}

			volumeRoots = unmodifiableSet(volumes);
		}
		return volumeRoots;
	}

	public Pattern getStructureRootPattern() throws IOException {
		if (structureRootFolderPattern == null) {
			List<String> folders = new ArrayList<String>();
			for (String it : queryBlacklistResource.get()) {
				if (it.startsWith("^") && it.endsWith("$")) {
					folders.add(it);
				}
			}
			structureRootFolderPattern = compile(join(folders, "|"), CASE_INSENSITIVE | UNICODE_CASE);
		}
		return structureRootFolderPattern;
	}

	public Pattern getLanguageTagPattern(Collection<String> languages) {
		// [en]
		return compile("(?<=[-\\[{(])(" + join(quoteAll(languages), "|") + ")(?=\\p{Punct})", CASE_INSENSITIVE | UNICODE_CASE);
	}

	public Pattern getLanguageSuffixPattern(Collection<String> languages, boolean strict) {
		// .en.srt
		return compile("(?<=[.])(" + join(quoteAll(languages), "|") + ")(?=[._ ]*$)", (strict ? 0 : CASE_INSENSITIVE) | UNICODE_CASE);
	}

	public Pattern getResolutionPattern() {
		// match screen resolutions 640x480, 1280x720, etc
		return compile("(?<!\\p{Alnum})(\\d{4}|[6-9]\\d{2})x(\\d{4}|[4-9]\\d{2})(?!\\p{Alnum})");
	}

	public Pattern getVideoFormatPattern(boolean strict) {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.video.format");
		return strict ? compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE) : compile(pattern, CASE_INSENSITIVE);
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

	public Pattern getReleaseGroupPattern(boolean strict) throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(releaseGroupResource.get(), "|") + ")(?!\\p{Alnum}|[^\\p{Alnum}]\\d{4})", strict ? 0 : CASE_INSENSITIVE | UNICODE_CASE);
	}

	public Pattern getBlacklistPattern() throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(queryBlacklistResource.get(), "|") + ")(?!\\p{Alnum})", CASE_INSENSITIVE | UNICODE_CASE);
	}

	public Pattern getExcludePattern() throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile(join(excludeBlacklistResource.get(), "|"), CASE_INSENSITIVE | UNICODE_CASE);
	}

	public Movie[] getMovieList() throws IOException {
		return movieListResource.get();
	}

	public TheTVDBSearchResult[] getTheTVDBIndex() throws IOException {
		return tvdbIndexResource.get();
	}

	public AnidbSearchResult[] getAnidbIndex() throws IOException {
		return anidbIndexResource.get();
	}

	private Map<Pattern, String> seriesDirectMappings;

	public Map<Pattern, String> getSeriesDirectMappings() throws IOException {
		if (seriesDirectMappings == null) {
			Map<Pattern, String> mappings = new LinkedHashMap<Pattern, String>();
			for (String line : seriesDirectMappingsResource.get()) {
				String[] tsv = line.split("\t", 2);
				if (tsv.length == 2) {
					mappings.put(compile("(?<!\\p{Alnum})(" + tsv[0] + ")(?!\\p{Alnum})", CASE_INSENSITIVE | UNICODE_CASE), tsv[1]);
				}
			}
			seriesDirectMappings = unmodifiableMap(mappings);
		}
		return seriesDirectMappings;
	}

	public FileFilter getDiskFolderFilter() {
		return new FolderEntryFilter(compile(getBundle(getClass().getName()).getString("pattern.diskfolder.entry")));
	}

	public FileFilter getDiskFolderEntryFilter() {
		return new RegexFileFilter(compile(getBundle(getClass().getName()).getString("pattern.diskfolder.entry")));
	}

	public FileFilter getClutterFileFilter() throws IOException {
		return new ClutterFileFilter(getExcludePattern(), Long.parseLong(getBundle(getClass().getName()).getString("number.clutter.maxfilesize"))); // only files smaller than 250 MB may be considered clutter
	}

	public List<File> getMediaRoots() {
		List<File> roots = new ArrayList<File>();
		for (String it : getBundle(getClass().getName()).getString("folder.media.roots").split(":")) {
			roots.add(new File(it));
		}
		return roots;
	}

	// fetch release group names online and try to update the data every other day
	protected final CachedResource<String[]> releaseGroupResource = new PatternResource(getBundle(getClass().getName()).getString("url.release-groups"));
	protected final CachedResource<String[]> queryBlacklistResource = new PatternResource(getBundle(getClass().getName()).getString("url.query-blacklist"));
	protected final CachedResource<String[]> excludeBlacklistResource = new PatternResource(getBundle(getClass().getName()).getString("url.exclude-blacklist"));
	protected final CachedResource<Movie[]> movieListResource = new MovieResource(getBundle(getClass().getName()).getString("url.movie-list"));
	protected final CachedResource<String[]> seriesDirectMappingsResource = new PatternResource(getBundle(getClass().getName()).getString("url.series-mappings"));
	protected final CachedResource<TheTVDBSearchResult[]> tvdbIndexResource = new TheTVDBIndexResource(getBundle(getClass().getName()).getString("url.thetvdb-index"));
	protected final CachedResource<AnidbSearchResult[]> anidbIndexResource = new AnidbIndexResource(getBundle(getClass().getName()).getString("url.anidb-index"));

	protected static class PatternResource extends CachedResource<String[]> {

		public PatternResource(String resource) {
			super(resource, String[].class, ONE_WEEK); // check for updates every week
		}

		@Override
		public String[] process(ByteBuffer data) {
			return compile("\\n").split(Charset.forName("UTF-8").decode(data));
		}
	}

	protected static class MovieResource extends CachedResource<Movie[]> {

		public MovieResource(String resource) {
			super(resource, Movie[].class, ONE_MONTH); // check for updates every month
		}

		@Override
		public Movie[] process(ByteBuffer data) throws IOException {
			List<String[]> rows = readCSV(new XZInputStream(new ByteBufferInputStream(data)), "UTF-8", "\t");
			List<Movie> movies = new ArrayList<Movie>(rows.size());

			for (String[] row : rows) {
				int imdbid = parseInt(row[0]);
				int tmdbid = parseInt(row[1]);
				int year = parseInt(row[2]);
				String name = row[3];
				String[] aliasNames = copyOfRange(row, 4, row.length);
				movies.add(new Movie(name, aliasNames, year, imdbid > 0 ? imdbid : -1, tmdbid > 0 ? tmdbid : -1));
			}

			return movies.toArray(new Movie[0]);
		}
	}

	protected static class TheTVDBIndexResource extends CachedResource<TheTVDBSearchResult[]> {

		public TheTVDBIndexResource(String resource) {
			super(resource, TheTVDBSearchResult[].class, ONE_WEEK); // check for updates every week
		}

		@Override
		public TheTVDBSearchResult[] process(ByteBuffer data) throws IOException {
			List<String[]> rows = readCSV(new XZInputStream(new ByteBufferInputStream(data)), "UTF-8", "\t");
			List<TheTVDBSearchResult> tvshows = new ArrayList<TheTVDBSearchResult>(rows.size());

			for (String[] row : rows) {
				int id = parseInt(row[0]);
				String name = row[1];
				String[] aliasNames = copyOfRange(row, 2, row.length);
				tvshows.add(new TheTVDBSearchResult(name, aliasNames, id));
			}

			return tvshows.toArray(new TheTVDBSearchResult[0]);
		}
	}

	protected static class AnidbIndexResource extends CachedResource<AnidbSearchResult[]> {

		public AnidbIndexResource(String resource) {
			super(resource, AnidbSearchResult[].class, ONE_MONTH); // check for updates every month
		}

		@Override
		public AnidbSearchResult[] process(ByteBuffer data) throws IOException {
			List<String[]> rows = readCSV(new XZInputStream(new ByteBufferInputStream(data)), "UTF-8", "\t");
			List<AnidbSearchResult> anime = new ArrayList<AnidbSearchResult>(rows.size());

			for (String[] row : rows) {
				int aid = parseInt(row[0]);
				String primaryTitle = row[1];
				String[] aliasNames = copyOfRange(row, 2, row.length);
				anime.add(new AnidbSearchResult(aid, primaryTitle, aliasNames));
			}

			return anime.toArray(new AnidbSearchResult[0]);
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

	public static class ClutterFileFilter extends FileFolderNameFilter {

		private long maxFileSize;

		public ClutterFileFilter(Pattern namePattern, long maxFileSize) {
			super(namePattern);
			this.maxFileSize = maxFileSize;
		}

		@Override
		public boolean accept(File file) {
			return super.accept(file) && file.isFile() && file.length() < maxFileSize;
		}
	}

	private Collection<String> quoteAll(Collection<String> strings) {
		List<String> patterns = new ArrayList<String>(strings.size());
		for (String it : strings) {
			patterns.add(Pattern.quote(it));
		}
		return patterns;
	}

	private Map<String, Locale> getLanguageMap(Locale... supportedDisplayLocale) {
		// use maximum strength collator by default
		Collator collator = Collator.getInstance(Locale.ROOT);
		collator.setDecomposition(Collator.FULL_DECOMPOSITION);
		collator.setStrength(Collator.PRIMARY);

		@SuppressWarnings("unchecked")
		Comparator<String> order = (Comparator) collator;
		Map<String, Locale> languageMap = new TreeMap<String, Locale>(order);

		for (String code : Locale.getISOLanguages()) {
			Locale locale = new Locale(code); // force ISO3 language as default toString() value
			Locale iso3locale = new Locale(locale.getISO3Language());

			languageMap.put(locale.getLanguage(), iso3locale);
			languageMap.put(locale.getISO3Language(), iso3locale);

			// map display language names for given locales
			for (Locale language : new HashSet<Locale>(asList(supportedDisplayLocale))) {
				// make sure language name is properly normalized so accents and whatever don't break the regex pattern syntax
				String languageName = Normalizer.normalize(locale.getDisplayLanguage(language), Form.NFKD);
				languageMap.put(languageName.toLowerCase(), iso3locale);
			}
		}

		// unofficial language for pb/pob for Portuguese (Brazil)
		Locale brazil = new Locale("pob");
		languageMap.put("pb", brazil);
		languageMap.put("pob", brazil);

		// remove illegal tokens
		languageMap.remove("");
		languageMap.remove("II");
		languageMap.remove("III");
		languageMap.remove("hi"); // hi => hearing-impaired subtitles, NOT hindi language

		Map<String, Locale> result = unmodifiableMap(languageMap);
		return result;
	}
}
