
package net.sourceforge.filebot.similarity;


import static java.lang.Math.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.Movie;

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
				return episode.airdate();
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
				String normalizedToken = normalizeObject(e.getTitle());
				if (normalizedToken.length() >= 3 && !normalizeObject(e.getSeriesName()).contains(normalizedToken)) {
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
			
			// 1:SxE && Title, 2:SxE
			return (float) ((max(sxe, 0) * title) + (floor(sxe) / 10));
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
				return new Object[] { removeTrailingBrackets(episode.getSeriesName()), episode.getTitle() };
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
			// normalize absolute similarity to similarity rank (4 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (floor(super.getSimilarity(o1, o2) * 4) / 4);
		}
		
		
		@Override
		protected String normalize(Object object) {
			if (object instanceof Episode) {
				object = removeTrailingBrackets(((Episode) object).getSeriesName());
			} else if (object instanceof Movie) {
				object = ((Movie) object).getName();
			} else if (object instanceof File) {
				object = getNameWithoutExtension(getRelativePathTail((File) object, 3).getPath());
			}
			// simplify file name, if possible
			return normalizeObject(object);
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
	AbsoluteName(new NameSimilarityMetric() {
		
		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return normalizeObject(object);
		}
	}),
	
	NumericSequence(new SequenceMatchSimilarity() {
		
		@Override
		protected String normalize(Object object) {
			if (object instanceof Episode) {
				object = EpisodeFormat.SeasonEpisode.formatSxE((Episode) object);
			} else if (object instanceof Movie) {
				object = ((Movie) object).getYear();
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
			float sum = 0;
			for (String s1 : f1) {
				for (String s2 : f2) {
					sum += super.getSimilarity(s1, s2);
				}
			}
			return sum / (f1.length * f2.length);
		}
		
		
		protected String[] fields(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				return new String[] { EpisodeFormat.SeasonEpisode.formatSxE(episode), String.valueOf(episode.getAbsolute()) };
			}
			
			if (object instanceof Movie) {
				Movie movie = (Movie) object;
				return new String[] { String.valueOf(movie.getYear()) };
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
			return (float) (floor(super.getSimilarity(o1, o2) * 40) / 40);
		}
		
		
		@Override
		public long getTimeStamp(Object object) {
			if (object instanceof Episode) {
				try {
					return ((Episode) object).airdate().getTimeStamp();
				} catch (RuntimeException e) {
					return -1; // some episodes may not have airdate defined
				}
			}
			
			return super.getTimeStamp(object);
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
					return super.getProperties(new net.sourceforge.filebot.media.MetaAttributes((File) object).getMetaData());
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
			return new SimilarityMetric[] { FileSize, new MetricCascade(FileName, EpisodeFunnel), EpisodeBalancer, SubstringFields, MetaAttributes, new MetricCascade(NameSubstringSequence, Name), Numeric, NumericSequence, Name, TimeStamp, AbsoluteName };
		} else {
			return new SimilarityMetric[] { EpisodeFunnel, EpisodeBalancer, SubstringFields, MetaAttributes, new MetricCascade(NameSubstringSequence, Name), Numeric, NumericSequence, Name, TimeStamp, AbsoluteName };
		}
	}
	
	
	public static SimilarityMetric verificationMetric() {
		return new MetricCascade(FileSize, FileName, SeasonEpisode, AirDate, Title, Name);
	}
	
}
