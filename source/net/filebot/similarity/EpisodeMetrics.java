package net.filebot.similarity;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.regex.Pattern.*;
import static java.util.stream.Collectors.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.media.XattrMetaInfo.*;
import static net.filebot.similarity.Normalization.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.StringUtilities.*;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.Transliterator;

import net.filebot.media.SmartSeasonEpisodeMatcher;
import net.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.filebot.vfs.FileInfo;
import net.filebot.web.Episode;
import net.filebot.web.EpisodeFormat;
import net.filebot.web.Movie;
import net.filebot.web.SeriesInfo;
import net.filebot.web.SimpleDate;
import one.util.streamex.StreamEx;

public enum EpisodeMetrics implements SimilarityMetric {

	// Match by season / episode numbers
	SeasonEpisode(new SeasonEpisodeMetric(new SmartSeasonEpisodeMatcher(null, false)) {

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

				// get SxE from episode, both SxE for season/episode numbering and SxE for absolute episode numbering
				Set<SxE> sxe = new HashSet<SxE>(2);

				// default SxE numbering
				if (episode.getEpisode() != null) {
					sxe.add(new SxE(episode.getSeason(), episode.getEpisode()));
				}
				// absolute numbering
				if (episode.getAbsolute() != null) {
					sxe.add(new SxE(null, episode.getAbsolute()));
				}
				// 0xSpecial numbering
				if (episode.getSpecial() != null) {
					sxe.add(new SxE(0, episode.getSpecial()));
				}
				result = sxe;
			} else {
				result = super.parse(object);
			}

			transformCache.put(object, result);
			return result;
		}
	}),

	// Match episode airdate
	AirDate(new DateMetric(getDateMatcher()) {

		private final Map<Object, SimpleDate> transformCache = synchronizedMap(new HashMap<Object, SimpleDate>(64, 4));

		@Override
		public SimpleDate parse(Object object) {
			if (object instanceof Movie) {
				return null;
			}

			if (object instanceof Episode) {
				Episode episode = (Episode) object;

				// use airdate from episode
				return episode.getAirdate();
			}

			SimpleDate result = transformCache.get(object);
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
				return normalizeObject(((Movie) object).getName());
			}

			String s = normalizeObject(object);
			return s.length() >= 4 ? s : null; // only consider long enough strings to avoid false matches
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
			float title = sxe < 1 ? Title.getSimilarity(o1, o2) : 1; // if SxE matches then boost score as if it was a title match as well

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
			return (float) ((Math.max(sxe, 0) * title) + (Math.floor(sxe) / 10));
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
			return (float) (Math.ceil(sum * 3) / 3);
		}

		protected String[] normalize(Object[] objects) {
			// normalize objects (and make sure to keep word boundaries)
			return stream(objects).map(EpisodeMetrics::normalizeObject).toArray(String[]::new);
		}

		protected Object[] fields(Object object) {
			if (object instanceof Episode) {
				Episode e = (Episode) object;
				return StreamEx.of(e.getSeriesName(), e.getTitle()).append(e.getSeriesNames()).map(Normalization::removeTrailingBrackets).distinct().limit(5).toArray();
			}

			if (object instanceof File) {
				File f = (File) object;
				return new Object[] { f.getParentFile().getAbsolutePath(), f };
			}

			if (object instanceof Movie) {
				Movie m = (Movie) object;
				return new Object[] { m.getName(), m.getYear() };
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
					max = Math.max(super.getSimilarity(s1, s2), max);
				}
			}

			// normalize absolute similarity to similarity rank (4 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (Math.floor(max * 4) / 4);
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
				return ((Episode) object).getSeriesNames();
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
			return (float) (Math.floor(super.getSimilarity(o1, o2) * 4) / 4);
		}

		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return normalizeObject(object);
		}
	}),

	// Match by generic name similarity (absolute)
	SeriesName(new NameSimilarityMetric() {

		private final SeriesNameMatcher seriesNameMatcher = getSeriesNameMatcher(false);

		@Override
		public float getSimilarity(Object o1, Object o2) {
			String[] f1 = getNormalizedEffectiveIdentifiers(o1);
			String[] f2 = getNormalizedEffectiveIdentifiers(o2);

			// match all fields and average similarity
			float max = 0;
			for (String s1 : f1) {
				for (String s2 : f2) {
					max = Math.max(super.getSimilarity(s1, s2), max);
				}
			}

			// normalize absolute similarity to similarity rank (4 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (Math.floor(max * 4) / 4);
		}

		@Override
		protected String normalize(Object object) {
			return object.toString();
		}

		protected String[] getNormalizedEffectiveIdentifiers(Object object) {
			return getEffectiveIdentifiers(object).stream().map(EpisodeMetrics::normalizeObject).toArray(String[]::new);
		}

		protected List<?> getEffectiveIdentifiers(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;

				// strip release info from known series name to make sure it matches the stripped filename
				return stripReleaseInfo(episode.getSeriesNames(), true);
			} else if (object instanceof File) {
				File file = (File) object;

				// guess potential series names from path
				return listPathTail(file, 3, true).stream().map(f -> {
					String fn = getName(f);
					String sn = seriesNameMatcher.matchByEpisodeIdentifier(fn);
					return sn != null ? sn : fn;
				}).collect(collectingAndThen(toList(), v -> stripReleaseInfo(v, true)));
			}

			return emptyList();
		}
	}),

	SeriesNameBalancer(new MetricCascade(NameSubstringSequence, Name, SeriesName)),

	// Match by generic name similarity (absolute)
	FilePath(new NameSimilarityMetric() {

		@Override
		protected String normalize(Object object) {
			if (object instanceof File) {
				object = normalizePathSeparators(getRelativePathTail((File) object, 3).getPath());
			}
			return normalizeObject(object.toString()); // simplify file name, if possible
		}
	}),

	FilePathBalancer(new NameSimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			String s1 = normalizeObject(o1);
			String s2 = normalizeObject(o2);

			s1 = stripReleaseInfo(s1, false);
			s2 = stripReleaseInfo(s2, false);

			int length = Math.min(s1.length(), s2.length());
			s1 = s1.substring(0, length);
			s2 = s2.substring(0, length);

			return (float) (Math.floor(super.getSimilarity(s1, s2) * 4) / 4);
		};

		@Override
		protected String normalize(Object object) {
			return object.toString();
		}
	}),

	NumericSequence(new SequenceMatchSimilarity() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			float lowerBound = super.getSimilarity(normalize(o1, true), normalize(o2, true));
			float upperBound = super.getSimilarity(normalize(o1, false), normalize(o2, false));

			return Math.max(lowerBound, upperBound);
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
			List<Integer> numbers = matchIntegers(normalizeObject(object));
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
						max = Math.max(super.getSimilarity(s1, s2), max);
						if (max >= 1) {
							return max;
						}
					}
				}
			}
			return max;
		}

		protected String[] fields(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				String[] f = new String[3];
				f[0] = episode.getSeriesName();
				f[1] = episode.getSpecial() == null ? EpisodeFormat.SeasonEpisode.formatSxE(episode) : episode.getSpecial().toString();
				f[2] = episode.getAbsolute() == null ? null : episode.getAbsolute().toString();
				return f;
			}

			if (object instanceof Movie) {
				Movie movie = (Movie) object;
				return new String[] { movie.getName(), String.valueOf(movie.getYear()) };
			}

			return new String[] { normalizeObject(object) };
		}
	}),

	// Prioritize proper episodes over specials
	SpecialNumber(new SimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			return getSpecialFactor(o1) + getSpecialFactor(o2);
		}

		public int getSpecialFactor(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				return episode.getSpecial() != null ? -1 : 1;
			}
			return 0;
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
			return f >= 0.9 ? 1 : f >= 0 ? 0 : -1;
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
			float r1 = getScore(o1);
			float r2 = getScore(o2);

			if (r1 < 0 || r2 < 0)
				return -1;

			return Math.max(r1, r2);
		}

		public float getScore(Object object) {
			if (object instanceof Episode) {
				SeriesInfo seriesInfo = ((Episode) object).getSeriesInfo();
				if (seriesInfo != null && seriesInfo.getRating() != null && seriesInfo.getRatingCount() != null) {
					if (seriesInfo.getRatingCount() >= 20) {
						return (float) Math.floor(seriesInfo.getRating() / 3); // BOOST POPULAR SHOWS and PUT INTO 3 GROUPS
					}
					if (seriesInfo.getRatingCount() >= 1) {
						return 0; // PENALIZE SHOWS WITH FEW RATINGS
					}
					return -1; // BIG PENALTY FOR SHOWS WITH 0 RATINGS
				}
			}
			return 0;
		}
	}),

	VoteRate(new SimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			float r1 = getScore(o1);
			float r2 = getScore(o2);

			return Math.max(r1, r2) >= 0.1 ? 1 : 0;
		}

		public float getScore(Object object) {
			if (object instanceof Episode) {
				SeriesInfo seriesInfo = ((Episode) object).getSeriesInfo();
				if (seriesInfo != null && seriesInfo.getRating() != null && seriesInfo.getRatingCount() != null && seriesInfo.getStartDate() != null) {
					long days = ChronoUnit.DAYS.between(seriesInfo.getStartDate().toLocalDate(), LocalDate.now());
					if (days > 0) {
						return (float) ((seriesInfo.getRatingCount().doubleValue() / days) * seriesInfo.getRating());
					}
				}
			}
			return 0;
		}
	}),

	// Match by (region) or (year) hints
	RegionHint(new SimilarityMetric() {

		private final Pattern hint = compile("[(](\\p{Alpha}+|\\p{Digit}+)[)]$");

		private final SeriesNameMatcher seriesNameMatcher = getSeriesNameMatcher(true);

		@Override
		public float getSimilarity(Object o1, Object o2) {
			Set<String> h1 = getHint(o1);
			Set<String> h2 = getHint(o2);

			return h1.isEmpty() || h2.isEmpty() ? 0 : h1.containsAll(h2) || h2.containsAll(h1) ? 1 : 0;
		}

		public Set<String> getHint(Object o) {
			if (o instanceof Episode) {
				for (String sn : ((Episode) o).getSeriesNames()) {
					Matcher m = hint.matcher(sn);
					if (m.find()) {
						return singleton(m.group(1).trim().toLowerCase());
					}
				}
			} else if (o instanceof File) {
				Set<String> h = new HashSet<String>();
				for (File f : listPathTail((File) o, 3, true)) {
					// try to focus on series name
					String fn = f.getName();
					String sn = seriesNameMatcher.matchByEpisodeIdentifier(fn);
					String[] tokens = PUNCTUATION_OR_SPACE.split(sn != null ? sn : fn);
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
			if (object instanceof File) {
				Object metaObject = xattr.getMetaInfo((File) object);
				if (metaObject != null) {
					return super.getProperties(metaObject);
				}
			}

			// ignore everything else
			return emptyMap();
		}

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

	public static String normalizeObject(Object object) {
		if (object == null) {
			return "";
		}

		String result = transformCache.get(object);
		if (result != null) {
			return result;
		}

		String name;
		if (object instanceof File) {
			name = getName((File) object);
		} else if (object instanceof FileInfo) {
			name = ((FileInfo) object).getName();
		} else {
			name = object.toString();
		}

		// remove checksums, any [...] or (...)
		name = removeEmbeddedChecksum(name);

		// remove obvious release info
		name = stripFormatInfo(name);

		synchronized (transliterator) {
			name = transliterator.transform(name);
		}

		// remove or normalize special characters
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
			return new SimilarityMetric[] { FileSize, new MetricCascade(FileName, EpisodeFunnel), EpisodeBalancer, AirDate, MetaAttributes, SubstringFields, SeriesNameBalancer, SeriesName, RegionHint, SpecialNumber, Numeric, NumericSequence, SeriesRating, VoteRate, TimeStamp, FilePathBalancer, FilePath };
		} else {
			return new SimilarityMetric[] { EpisodeFunnel, EpisodeBalancer, AirDate, MetaAttributes, SubstringFields, SeriesNameBalancer, SeriesName, RegionHint, SpecialNumber, Numeric, NumericSequence, SeriesRating, VoteRate, TimeStamp, FilePathBalancer, FilePath };
		}
	}

	public static SimilarityMetric verificationMetric() {
		return new MetricCascade(FileName, SeasonEpisode, AirDate, Title, Name);
	}

}
