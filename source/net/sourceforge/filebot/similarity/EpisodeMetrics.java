package net.sourceforge.filebot.similarity;

import static java.lang.Math.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.regex.Pattern.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.media.ReleaseInfo;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.TheTVDBClient.SeriesInfo;
import net.sourceforge.filebot.web.TheTVDBSearchResult;

import com.ibm.icu.text.Transliterator;

public enum EpisodeMetrics implements SimilarityMetric {

	// Match by season / episode numbers
	SeasonEpisode(new SeasonEpisodeMetric() {

		private final Map<Object, Collection<SxE>> transformCache = synchronizedMap(new HashMap<Object, Collection<SxE>>(64, 4));

		@Override
		protected Collection<SxE> parse(Object object) {
			if (object instanceof Movie) {
				return emptySet();
			}

			Collection<SxE> result = transformCache.get(object);
			if (result != null) {
				return result;
			}

			if (object instanceof Episode) {
				Episode episode = (Episode) object;

				if (episode.getSpecial() != null) {
					return singleton(new SxE(0, episode.getSpecial()));
				}

				// get SxE from episode, both SxE for season/episode numbering and SxE for absolute episode numbering
				SxE sxe = new SxE(episode.getSeason(), episode.getEpisode());
				SxE abs = new SxE(null, episode.getAbsolute());

				result = (abs.episode < 0 || sxe.equals(abs)) ? singleton(sxe) : asList(sxe, abs);
			} else {
				result = super.parse(object);
			}

			transformCache.put(object, result);
			return result;
		}
	}),

	// Match episode airdate
	AirDate(new DateMetric() {

		private final Map<Object, Date> transformCache = synchronizedMap(new HashMap<Object, Date>(64, 4));

		@Override
		public Date parse(Object object) {
			if (object instanceof Movie) {
				return null;
			}

			if (object instanceof Episode) {
				Episode episode = (Episode) object;

				// use airdate from episode
				return episode.getAirdate();
			}

			Date result = transformCache.get(object);
			if (result != null) {
				return result;
			}

			result = super.parse(object);
			transformCache.put(object, result);
			return result;
		}
	}),

	// Match by episode/movie title
	Title(new SubstringMetric() {

		@Override
		protected String normalize(Object object) {
			if (object instanceof Episode) {
				Episode e = (Episode) object;

				// don't use title for matching if title equals series name
				String normalizedToken = normalizeObject(removeTrailingBrackets(e.getTitle()));
				if (normalizedToken.length() >= 4 && !normalizeObject(e.getSeriesName()).contains(normalizedToken)) {
					return normalizedToken;
				}
			}

			if (object instanceof Movie) {
				object = ((Movie) object).getName();
			}

			return normalizeObject(object);
		}
	}),

	// Match by SxE and airdate
	EpisodeIdentifier(new MetricCascade(SeasonEpisode, AirDate)),

	// Advanced episode <-> file matching Lv1
	EpisodeFunnel(new MetricCascade(SeasonEpisode, AirDate, Title)),

	// Advanced episode <-> file matching Lv2
	EpisodeBalancer(new SimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			float sxe = EpisodeIdentifier.getSimilarity(o1, o2);
			float title = Title.getSimilarity(o1, o2);

			// account for misleading SxE patterns in the episode title
			if (sxe < 0 && title == 1 && EpisodeIdentifier.getSimilarity(getTitle(o1), getTitle(o2)) == 1) {
				sxe = 1;
				title = 0;
			}

			// allow title to override SxE only if series name also is a good match
			if (title == 1 && SeriesName.getSimilarity(o1, o2) < 0.5f) {
				title = 0;
			}

			// 1:SxE && Title, 2:SxE
			return (float) ((max(sxe, 0) * title) + (floor(sxe) / 10));
		}

		public Object getTitle(Object o) {
			if (o instanceof Episode) {
				Episode e = (Episode) o;
				return String.format("%s %s", e.getSeriesName(), e.getTitle());
			}
			return o;
		}
	}),

	// Match series title and episode title against folder structure and file name
	SubstringFields(new SubstringMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			String[] f1 = normalize(fields(o1));
			String[] f2 = normalize(fields(o2));

			// match all fields and average similarity
			float sum = 0;
			for (String s1 : f1) {
				for (String s2 : f2) {
					sum += super.getSimilarity(s1, s2);
				}
			}
			sum /= f1.length * f2.length;

			// normalize into 3 similarity levels
			return (float) (ceil(sum * 3) / 3);
		}

		protected String[] normalize(Object[] objects) {
			String[] names = new String[objects.length];
			for (int i = 0; i < objects.length; i++) {
				names[i] = normalizeObject(objects[i]).replaceAll("\\s", "");
			}
			return names;
		}

		protected Object[] fields(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				LinkedHashSet<String> set = new LinkedHashSet<String>(4);
				set.add(removeTrailingBrackets(episode.getSeriesName()));
				set.add(removeTrailingBrackets(episode.getTitle()));
				for (String it : episode.getSeries().getEffectiveNames()) {
					set.add(removeTrailingBrackets(it));
				}

				Iterator<String> itr = set.iterator();
				Object[] f = new Object[4];
				for (int i = 0; i < f.length; i++) {
					f[i] = itr.hasNext() ? itr.next() : null;
				}
				return f;
			}

			if (object instanceof File) {
				File file = (File) object;
				return new Object[] { file.getParentFile().getAbsolutePath(), file };
			}

			if (object instanceof Movie) {
				Movie movie = (Movie) object;
				return new Object[] { movie.getName(), movie.getYear() };
			}

			return new Object[] { object };
		}
	}),

	// Match via common word sequence in episode name and file name
	NameSubstringSequence(new SequenceMatchSimilarity() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			String[] f1 = getNormalizedEffectiveIdentifiers(o1);
			String[] f2 = getNormalizedEffectiveIdentifiers(o2);

			// match all fields and average similarity
			float max = 0;
			for (String s1 : f1) {
				for (String s2 : f2) {
					max = max(super.getSimilarity(s1, s2), max);
				}
			}

			// normalize absolute similarity to similarity rank (4 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (floor(max * 4) / 4);
		}

		@Override
		protected String normalize(Object object) {
			return object.toString();
		}

		protected String[] getNormalizedEffectiveIdentifiers(Object object) {
			List<?> identifiers = getEffectiveIdentifiers(object);
			String[] names = new String[identifiers.size()];

			for (int i = 0; i < names.length; i++) {
				names[i] = normalizeObject(identifiers.get(i));
			}
			return names;
		}

		protected List<?> getEffectiveIdentifiers(Object object) {
			if (object instanceof Episode) {
				return ((Episode) object).getSeries().getEffectiveNames();
			} else if (object instanceof Movie) {
				return ((Movie) object).getEffectiveNames();
			} else if (object instanceof File) {
				return listPathTail((File) object, 3, true);
			}
			return singletonList(object);
		}
	}),

	// Match by generic name similarity (round rank)
	Name(new NameSimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			// normalize absolute similarity to similarity rank (4 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (floor(super.getSimilarity(o1, o2) * 4) / 4);
		}

		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return normalizeObject(object);
		}
	}),

	// Match by generic name similarity (absolute)
	SeriesName(new NameSimilarityMetric() {

		private ReleaseInfo releaseInfo = new ReleaseInfo();
		private SeriesNameMatcher seriesNameMatcher = new SeriesNameMatcher(Locale.ROOT, false);

		@Override
		public float getSimilarity(Object o1, Object o2) {
			String[] f1 = getNormalizedEffectiveIdentifiers(o1);
			String[] f2 = getNormalizedEffectiveIdentifiers(o2);

			// match all fields and average similarity
			float max = 0;
			for (String s1 : f1) {
				for (String s2 : f2) {
					max = max(super.getSimilarity(s1, s2), max);
				}
			}

			// normalize absolute similarity to similarity rank (4 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (floor(max * 4) / 4);
		}

		@Override
		protected String normalize(Object object) {
			return object.toString();
		}

		protected String[] getNormalizedEffectiveIdentifiers(Object object) {
			List<?> identifiers = getEffectiveIdentifiers(object);
			String[] names = new String[identifiers.size()];

			for (int i = 0; i < names.length; i++) {
				names[i] = normalizeObject(identifiers.get(i));
			}
			return names;
		}

		protected List<?> getEffectiveIdentifiers(Object object) {
			List<String> names = null;

			if (object instanceof Episode) {
				names = ((Episode) object).getSeries().getEffectiveNames();
			} else if (object instanceof File) {
				names = new ArrayList<String>(3);
				for (File f : listPathTail((File) object, 3, true)) {
					String fn = getName(f);
					String sn = seriesNameMatcher.matchByEpisodeIdentifier(fn);
					if (sn != null) {
						names.add(sn);
					} else {
						names.add(fn);
					}
				}
			}

			// equally strip away strip potential any clutter
			if (names != null) {
				try {
					return releaseInfo.cleanRelease(names, false);
				} catch (NoSuchElementException e) {
					// keep default value in case all tokens are stripped away
				} catch (IOException e) {
					Logger.getLogger(EpisodeMetrics.class.getName()).log(Level.WARNING, e.getMessage());
				}
			}

			// simplify file name, if possible
			return emptyList();
		}
	}),

	// Match by generic name similarity (absolute)
	AbsolutePath(new NameSimilarityMetric() {

		@Override
		protected String normalize(Object object) {
			if (object instanceof File) {
				object = normalizePathSeparators(getRelativePathTail((File) object, 3).getPath());
			}
			return normalizeObject(object.toString()); // simplify file name, if possible
		}
	}),

	NumericSequence(new SequenceMatchSimilarity() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			float lowerBound = super.getSimilarity(normalize(o1, true), normalize(o2, true));
			float upperBound = super.getSimilarity(normalize(o1, false), normalize(o2, false));

			return max(lowerBound, upperBound);
		};

		@Override
		protected String normalize(Object object) {
			return object.toString();
		};

		protected String normalize(Object object, boolean numbersOnly) {
			if (object instanceof Episode) {
				Episode e = (Episode) object;
				if (numbersOnly) {
					object = EpisodeFormat.SeasonEpisode.formatSxE(e);
				} else {
					object = String.format("%s %s", e.getSeriesName(), EpisodeFormat.SeasonEpisode.formatSxE(e));
				}
			} else if (object instanceof Movie) {
				Movie m = (Movie) object;
				if (numbersOnly) {
					object = m.getYear();
				} else {
					object = String.format("%s %s", m.getName(), m.getYear());
				}
			}

			// simplify file name if possible and extract numbers
			List<Integer> numbers = new ArrayList<Integer>(4);
			Scanner scanner = new Scanner(normalizeObject(object)).useDelimiter("\\D+");
			while (scanner.hasNextInt()) {
				numbers.add(scanner.nextInt());
			}
			return join(numbers, " ");
		}
	}),

	// Match by generic numeric similarity
	Numeric(new NumericSimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			String[] f1 = fields(o1);
			String[] f2 = fields(o2);

			// match all fields and average similarity
			float max = 0;
			for (String s1 : f1) {
				for (String s2 : f2) {
					if (s1 != null && s2 != null) {
						max = max(super.getSimilarity(s1, s2), max);
					}
				}
			}
			return max;
		}

		protected String[] fields(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				String[] f = new String[4];
				f[0] = episode.getSeriesName();
				f[1] = EpisodeFormat.SeasonEpisode.formatSxE(episode);
				f[2] = episode.getAbsolute() == null ? null : episode.getAbsolute().toString();
				f[3] = episode.getSeason() == null || episode.getEpisode() == null ? null : String.format("%02d%02d", episode.getSeason(), episode.getEpisode());
				return f;
			}

			if (object instanceof Movie) {
				Movie movie = (Movie) object;
				return new String[] { movie.getName(), String.valueOf(movie.getYear()) };
			}

			return new String[] { normalizeObject(object) };
		}
	}),

	// Match by file length (only works when matching torrents or files)
	FileSize(new FileSizeMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			// order of arguments is logically irrelevant, but we might be able to save us a call to File.length() which is quite costly
			return o1 instanceof File ? super.getSimilarity(o2, o1) : super.getSimilarity(o1, o2);
		}

		@Override
		protected long getLength(Object object) {
			if (object instanceof FileInfo) {
				return ((FileInfo) object).getLength();
			}

			return super.getLength(object);
		}
	}),

	// Match by common words at the beginning of both files
	FileName(new FileNameMetric() {

		@Override
		protected String getFileName(Object object) {
			if (object instanceof File || object instanceof FileInfo) {
				return normalizeObject(object);
			}

			return null;
		}
	}),

	// Match by file last modified and episode release dates
	TimeStamp(new TimeStampMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			// adjust differentiation accuracy to about a year
			float f = super.getSimilarity(o1, o2);
			return f >= 0.8 ? 1 : f >= 0 ? 0 : -1;
		}

		@Override
		public long getTimeStamp(Object object) {
			if (object instanceof Episode) {
				try {
					long ts = ((Episode) object).getAirdate().getTimeStamp();

					// big penalty for episodes not yet aired
					if (ts > System.currentTimeMillis()) {
						return -1;
					}

					return ts;
				} catch (RuntimeException e) {
					return -1; // some episodes may not have airdate defined
				}
			}

			return super.getTimeStamp(object);
		}
	}),

	SeriesRating(new SimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			float r1 = getRating(o1);
			float r2 = getRating(o2);
			return max(r1, r2) >= 0.4 ? 1 : min(r1, r2) < 0 ? -1 : 0;
		}

		private final Map<String, SeriesInfo> seriesInfoCache = new HashMap<String, SeriesInfo>();

		public float getRating(Object o) {
			if (o instanceof Episode) {
				try {
					synchronized (seriesInfoCache) {
						String n = ((Episode) o).getSeriesName();

						SeriesInfo seriesInfo = seriesInfoCache.get(n);
						if (seriesInfo == null && !seriesInfoCache.containsKey(n)) {
							try {
								seriesInfo = WebServices.TheTVDB.getSeriesInfo((TheTVDBSearchResult) ((Episode) o).getSeries(), Locale.ENGLISH);
							} catch (Exception e) {
								seriesInfo = WebServices.TheTVDB.getSeriesInfoByLocalIndex(((Episode) o).getSeriesName(), Locale.ENGLISH);
							}
							seriesInfoCache.put(n, seriesInfo);
						}

						if (seriesInfo != null) {
							if (seriesInfo.getRatingCount() > 0) {
								float rating = max(0, seriesInfo.getRating().floatValue());
								return seriesInfo.getRatingCount() >= 15 ? rating : 0; // PENALIZE SHOWS WITH FEW RATINGS
							} else {
								return -1; // BIG PENALTY FOR SHOWS WITH 0 RATINGS
							}
						}
					}
				} catch (Exception e) {
					Logger.getLogger(EpisodeMetrics.class.getName()).log(Level.WARNING, e.getMessage());
				}
			}
			return 0;
		}
	}),

	// Match by (region) or (year) hints
	RegionHint(new SimilarityMetric() {

		private Pattern hint = compile("[(](\\p{Alpha}+|\\p{Digit}+)[)]$");

		private SeriesNameMatcher seriesNameMatcher = new SeriesNameMatcher();
		private Pattern punctuation = compile("[\\p{Punct}\\p{Space}]+");

		@Override
		public float getSimilarity(Object o1, Object o2) {
			Set<String> h1 = getHint(o1);
			Set<String> h2 = getHint(o2);

			return h1.isEmpty() || h2.isEmpty() ? 0 : h1.containsAll(h2) || h2.containsAll(h1) ? 1 : 0;
		}

		public Set<String> getHint(Object o) {
			if (o instanceof Episode) {
				Matcher m = hint.matcher(((Episode) o).getSeriesName());
				if (m.find()) {
					return singleton(m.group(1).trim().toLowerCase());
				}
			} else if (o instanceof File) {
				Set<String> h = new HashSet<String>();
				for (File f : listPathTail((File) o, 3, true)) {
					// try to focus on series name
					String n = f.getName();
					String sn = seriesNameMatcher.matchByEpisodeIdentifier(n);

					// tokenize
					String[] tokens = punctuation.split(sn != null ? sn : n);
					for (String s : tokens) {
						if (s.length() > 0) {
							h.add(s.trim().toLowerCase());
						}
					}
				}
				return h;
			}

			return emptySet();
		}
	}),

	// Match by stored MetaAttributes if possible
	MetaAttributes(new CrossPropertyMetric() {

		@Override
		protected Map<String, Object> getProperties(Object object) {
			// Episode / Movie objects
			if (object instanceof Episode || object instanceof Movie) {
				return super.getProperties(object);
			}

			// deserialize MetaAttributes if enabled and available
			if (object instanceof File && useExtendedFileAttributes()) {
				try {
					return super.getProperties(new net.sourceforge.filebot.media.MetaAttributes((File) object).getObject());
				} catch (Throwable e) {
					// ignore
				}
			}

			// ignore everything else
			return emptyMap();
		};

	});

	// inner metric
	private final SimilarityMetric metric;

	private EpisodeMetrics(SimilarityMetric metric) {
		this.metric = metric;
	}

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return metric.getSimilarity(o1, o2);
	}

	private static final Map<Object, String> transformCache = synchronizedMap(new HashMap<Object, String>(64, 4));
	private static final Transliterator transliterator = Transliterator.getInstance("Any-Latin;Latin-ASCII;[:Diacritic:]remove");

	protected static String normalizeObject(Object object) {
		if (object == null) {
			return "";
		}

		String result = transformCache.get(object);
		if (result != null) {
			return result;
		}

		String name = object.toString();

		// use name without extension
		if (object instanceof File) {
			name = getName((File) object);
		} else if (object instanceof FileInfo) {
			name = ((FileInfo) object).getName();
		}

		// remove checksums, any [...] or (...)
		name = removeEmbeddedChecksum(name);

		synchronized (transliterator) {
			name = transliterator.transform(name);
		}

		// remove/normalize special characters
		name = normalizePunctuation(name);

		// normalize to lower case
		name = name.toLowerCase();

		transformCache.put(object, name);
		return name;
	}

	public static SimilarityMetric[] defaultSequence(boolean includeFileMetrics) {
		// 1 pass: divide by file length (only works for matching torrent entries or files)
		// 2-3 pass: divide by title or season / episode numbers
		// 4 pass: divide by folder / file name and show name / episode title
		// 5 pass: divide by name (rounded into n levels)
		// 6 pass: divide by generic numeric similarity
		// 7 pass: prefer episodes that were aired closer to the last modified date of the file
		// 8 pass: resolve remaining collisions via absolute string similarity
		if (includeFileMetrics) {
			return new SimilarityMetric[] { FileSize, new MetricCascade(FileName, EpisodeFunnel), EpisodeBalancer, AirDate, MetaAttributes, SubstringFields, new MetricCascade(NameSubstringSequence, Name), Numeric, NumericSequence, SeriesName, RegionHint, SeriesRating, TimeStamp, AbsolutePath };
		} else {
			return new SimilarityMetric[] { EpisodeFunnel, EpisodeBalancer, AirDate, MetaAttributes, SubstringFields, new MetricCascade(NameSubstringSequence, Name), Numeric, NumericSequence, SeriesName, RegionHint, SeriesRating, TimeStamp, AbsolutePath };
		}
	}

	public static SimilarityMetric verificationMetric() {
		return new MetricCascade(FileName, SeasonEpisode, AirDate, Title, Name);
	}

}
