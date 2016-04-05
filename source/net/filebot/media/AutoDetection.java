package net.filebot.media;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.regex.Pattern.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.similarity.Normalization.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.StringUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.filebot.WebServices;
import net.filebot.format.ExpressionFormatMethods;
import net.filebot.similarity.NameSimilarityMetric;
import net.filebot.util.FastFile;
import net.filebot.web.Movie;

public class AutoDetection {

	private File[] files;
	private Locale locale;

	public AutoDetection(Collection<File> root, Locale locale) {
		this.files = resolve(root.stream().map(FastFile::new), getSystemFilesFilter()).toArray(File[]::new);
		this.locale = locale;
	}

	protected Stream<File> resolve(Stream<File> root, FileFilter excludes) {
		return root.flatMap(f -> {
			if (f.isHidden() || excludes.accept(f)) {
				return Stream.empty();
			}
			if (f.isFile()) {
				return Stream.of(f);
			}
			if (f.isDirectory()) {
				return isDiskFolder(f) ? Stream.of(f) : resolve(getChildren(f).stream(), excludes);
			}
			return Stream.empty();
		});
	}

	public List<File> getFiles() {
		return unmodifiableList(asList(files));
	}

	private static final Pattern MOVIE_PATTERN = Pattern.compile("[\\\\/]Movies[\\\\/]", CASE_INSENSITIVE);
	private static final Pattern SERIES_PATTERN = Pattern.compile("[\\\\/](?:TV.Shows|TV.Series)[\\\\/]|tv[sp]-|EP\\d{1,3}|Season\\D?\\d{1,2}|\\d{4}.S\\d{2}", CASE_INSENSITIVE);
	private static final Pattern ANIME_PATTERN = Pattern.compile("[\\\\/]Anime[\\\\/]|[\\(\\[]\\p{XDigit}{8}[\\]\\)]|[\\[][A-Z]+Subs[\\]]", CASE_INSENSITIVE);

	private Predicate<File> forceIgnore = f -> false;
	private Predicate<File> forceMusic = f -> AUDIO_FILES.accept(f) && !VIDEO_FILES.accept(f);
	private Predicate<File> forceMovie = f -> find(f.getPath(), MOVIE_PATTERN) || isMovie(f, true);
	private Predicate<File> forceSeries = f -> find(f.getPath(), SERIES_PATTERN) || isEpisode(f, true);
	private Predicate<File> forceAnime = f -> find(f.getPath(), ANIME_PATTERN);

	public Map<Group, Set<File>> group() {
		// sort keys and values
		Map<Group, Set<File>> groups = new TreeMap<Group, Set<File>>();

		stream(files).parallel().collect(groupingByConcurrent(f -> {
			Group group = new Group();
			try {
				if (forceIgnore.test(f)) {
					return group;
				}
				if (forceMusic.test(f)) {
					return group.music(f);
				}
				if (forceMovie.test(f)) {
					return group.movie(getMovieMatches(f, false));
				}
				if (forceSeries.test(f)) {
					return group.series(getSeriesMatches(f, false));
				}
				if (forceAnime.test(f)) {
					return group.anime(getSeriesMatches(f, true));
				}
				return detectGroup(f);
			} catch (Exception e) {
				debug.log(Level.SEVERE, e.getMessage(), e);
			}
			return group;
		})).forEach((k, v) -> groups.put(k, new TreeSet<File>(v)));

		return groups;
	}

	private List<String> getSeriesMatches(File f, boolean anime) throws Exception {
		List<String> names = detectSeriesNames(singleton(f), anime, locale);
		if (names.size() > 0) {
			return names;
		}
		List<File> episodes = getVideoFiles(f.getParentFile());
		if (episodes.size() >= 5) {
			return detectSeriesNames(episodes, anime, locale);
		}
		return emptyList();
	}

	private List<Movie> getMovieMatches(File file, boolean strict) throws Exception {
		return MediaDetection.detectMovie(file, WebServices.TheMovieDB, locale, strict);
	}

	private List<File> getVideoFiles(File parent) {
		return stream(files).filter(it -> parent.equals(it.getParentFile())).filter(VIDEO_FILES::accept).collect(toList());
	}

	private Group detectGroup(File f) throws Exception {
		Group group = new Group();

		List<String> s = getSeriesMatches(f, false);
		List<Movie> m = getMovieMatches(f, false);

		if (m.isEmpty() && s.isEmpty())
			return group;
		if (s.size() > 0 && m.isEmpty())
			return group.series(s);
		if (m.size() > 0 && s.isEmpty())
			return group.movie(m);

		log.fine(format("%s [series: %s, movie: %s]", f.getName(), s.get(0), m.get(0)));

		return new Rules(f, s, m).apply();
	}

	private static final Pattern YEAR = Pattern.compile("\\D(?:19|20)\\d{2}\\D");
	private static final Pattern EPISODE_NUMBERS = Pattern.compile("\\b\\d{1,3}\\b");
	private static final Pattern DASH = Pattern.compile("^.{0,3}\\s[-]\\s.+$", UNICODE_CHARACTER_CLASS);
	private static final Pattern NUMBER_PAIR = Pattern.compile("\\D\\d{1,2}\\D{1,3}\\d{1,2}\\D");
	private static final Pattern NON_NUMBER_NAME = Pattern.compile("^[\\p{L}\\p{Space}\\p{Punct}]+$", UNICODE_CHARACTER_CLASS);

	private class Rules {

		private final Group group;

		private final File f;
		private final String s;
		private final Movie m;

		private final String dn, fn, sn, mn, my, asn;
		private final Pattern snm, mnm;

		public Rules(File file, List<String> series, List<Movie> movie) throws Exception {
			group = new Group().series(series).movie(movie);

			f = file;
			s = series.get(0);
			m = movie.get(0);

			dn = normalize(getName(guessMovieFolder(f)));
			fn = normalize(getName(f));
			sn = normalize(s);
			mn = normalize(m.getName());
			my = Integer.toString(m.getYear());

			snm = compile(sn, LITERAL);
			mnm = compile(mn, LITERAL);
			asn = after(fn, snm).orElse(fn);
		}

		private String normalize(String self) {
			return self == null ? "" : replaceSpace(normalizePunctuation(ExpressionFormatMethods.ascii(self)).toLowerCase(), " ").trim();
		}

		private float getSimilarity(String self, String other) {
			return new NameSimilarityMetric().getSimilarity(self, other);
		}

		public Group apply() throws Exception {
			List<Rule> rules = new ArrayList<Rule>(15);
			rules.add(new Rule(-1, 0, this::equalsMovieName));
			rules.add(new Rule(-1, 0, this::containsMovieYear));
			rules.add(new Rule(-1, 0, this::containsMovieNameYear));
			rules.add(new Rule(5, -1, this::containsEpisodeNumbers));
			rules.add(new Rule(5, -1, this::commonNumberPattern));
			rules.add(new Rule(1, -1, this::episodeWithoutNumbers));
			rules.add(new Rule(1, -1, this::episodeNumbers));
			rules.add(new Rule(-1, 1, this::hasImdbId));
			rules.add(new Rule(-1, 1, this::nonNumberName));
			rules.add(new Rule(-1, 5, this::exactMovieMatch));
			rules.add(new Rule(-1, 1, this::containsMovieName));
			rules.add(new Rule(-1, 1, this::similarNameYear));
			rules.add(new Rule(-1, 1, this::similarNameNoNumbers));
			rules.add(new Rule(-1, 1, this::aliasNameMatch));

			int score_s = 0;
			int score_m = 0;
			for (Rule rule : rules) {
				if (rule.test()) {
					score_s += rule.s;
					score_m += rule.m;

					if (score_s >= 1 && score_m <= -1) {
						return group.movie(null);
					}
					if (score_m >= 1 && score_s <= -1) {
						return group.series(null);
					}
				}
			}
			return group;
		}

		public boolean equalsMovieName() {
			return mn.equals(fn);
		}

		public boolean containsMovieYear() {
			return m.getYear() >= 1950 && listPathTail(f, 3, true).stream().anyMatch(it -> it.getName().contains(my) && parseEpisodeNumber(it.getName(), false) == null);
		}

		public boolean containsMovieNameYear() {
			return mn.equals(sn) && Stream.of(dn, fn).anyMatch(it -> {
				return parseEpisodeNumber(after(it, YEAR).orElse(""), false) == null;
			});
		}

		public boolean containsEpisodeNumbers() {
			return parseEpisodeNumber(fn, true) != null || parseDate(fn) != null;
		}

		public boolean commonNumberPattern() {
			return getChildren(f.getParentFile()).stream().filter(VIDEO_FILES::accept).filter(it -> {
				return find(dn, snm) || find(normalize(it.getName()), snm);
			}).map(it -> {
				return streamMatches(it.getName(), EPISODE_NUMBERS).map(Integer::new).collect(toSet());
			}).filter(it -> it.size() > 0).distinct().count() >= 10;
		}

		public boolean episodeWithoutNumbers() throws Exception {
			return find(asn, DASH) && getMovieMatches(f, true).isEmpty();
		}

		public boolean episodeNumbers() throws Exception {
			String n = stripReleaseInfo(asn, false);
			if (parseEpisodeNumber(n, false) != null || NUMBER_PAIR.matcher(n).find()) {
				return Stream.of(dn, fn).anyMatch(it -> snm.matcher(it).find()) && getMovieMatches(f, true).isEmpty();
			}
			return false;
		}

		public boolean hasImdbId() {
			return grepImdbId(fn).size() > 0;
		}

		public boolean nonNumberName() {
			return find(getName(f), NON_NUMBER_NAME);
		}

		public boolean exactMovieMatch() throws Exception {
			return getMovieMatches(f, true).size() > 0 && Stream.of(dn, fn).anyMatch(it -> find(it, YEAR));
		}

		public boolean containsMovieName() {
			return fn.contains(mn) && parseEpisodeNumber(after(fn, mnm).orElse(fn), false) == null;
		}

		public boolean similarNameYear() {
			return getSimilarity(mn, fn) >= 0.8f || Stream.of(dn, fn).anyMatch(it -> {
				return matchIntegers(it).stream().filter(y -> m.getYear() - 1 <= y && y <= m.getYear() + 1).count() > 0;
			});
		}

		public boolean similarNameNoNumbers() {
			return Stream.of(dn, fn).anyMatch(it -> {
				return find(it, mnm) && !find(after(it, mnm).orElse(it), EPISODE_NUMBERS) && getSimilarity(it, mn) >= 0.2f + getSimilarity(it, sn);
			});
		}

		public boolean aliasNameMatch() {
			return m.getEffectiveNamesWithoutYear().stream().map(this::normalize).anyMatch(fn::contains);
		}

	}

	@FunctionalInterface
	private interface Test {
		boolean test() throws Exception;
	}

	private static class Rule implements Test {

		public final int s;
		public final int m;

		private final Test t;

		public Rule(int s, int m, Test t) {
			this.s = s;
			this.m = m;
			this.t = t;
		}

		@Override
		public boolean test() throws Exception {
			return t.test();
		}
	}

	public enum Type {
		Movie, Series, Anime, Music;
	}

	public static class Group extends EnumMap<Type, Object> implements Comparable<Group> {

		public Group() {
			super(Type.class);
		}

		public Object getMovie() {
			return get(Type.Movie);
		}

		public Object getSeries() {
			return get(Type.Series);
		}

		public Object getAnime() {
			return get(Type.Anime);
		}

		public Object getMusic() {
			return get(Type.Music);
		}

		public Group movie(List<Movie> movies) {
			put(Type.Movie, movies == null || movies.isEmpty() ? null : movies.get(0));
			return this;
		}

		public Group series(List<String> names) {
			put(Type.Series, names == null || names.isEmpty() ? null : names.get(0));
			return this;

		}

		public Group anime(List<String> names) {
			put(Type.Anime, names == null || names.isEmpty() ? null : names.get(0));
			return this;

		}

		public Group music(File f) {
			put(Type.Music, f == null ? null : f.getParent());
			return this;
		}

		@Override
		public int compareTo(Group other) {
			if (size() != other.size()) {
				return Integer.compare(size(), other.size());
			}

			return stream(Type.values()).mapToInt(t -> {
				return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER).compare(Objects.toString(get(t), null), Objects.toString(other.get(t), null));
			}).filter(i -> i != 0).findFirst().orElse(0);
		}

	}

}
