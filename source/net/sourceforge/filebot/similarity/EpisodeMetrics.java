
package net.sourceforge.filebot.similarity;


import static java.lang.Math.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.hash.VerificationUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.Movie;


public enum EpisodeMetrics implements SimilarityMetric {
	
	// Match by season / episode numbers
	SeasonEpisode(new SeasonEpisodeMetric() {
		
		private final Map<Object, Collection<SxE>> transformCache = synchronizedMap(new WeakHashMap<Object, Collection<SxE>>(64, 4));
		
		
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
		
		private final Map<Object, Date> transformCache = synchronizedMap(new WeakHashMap<Object, Date>(64, 4));
		
		
		@Override
		protected Date parse(Object object) {
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
				object = ((Episode) object).getTitle();
			}
			
			if (object instanceof Movie) {
				object = ((Movie) object).getName();
			}
			
			return normalizeObject(object);
		}
	}),
	
	// Match by SxE and airdate
	EpisodeIdentifier(new MetricCascade(SeasonEpisode, AirDate)),
	
	// Advanced episode <-> file matching
	EpisodeFunnel(new MetricCascade(SeasonEpisode, AirDate, Title)),
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
				names[i] = normalizeObject(objects[i]);
			}
			
			return names;
		}
		
		
		protected Object[] fields(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				return new Object[] { episode.getSeriesName(), episode.getTitle() };
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
	
	// Match by generic name similarity
	Name(new NameSimilarityMetric() {
		
		@Override
		public float getSimilarity(Object o1, Object o2) {
			// normalize absolute similarity to similarity rank (6 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (floor(super.getSimilarity(o1, o2) * 6) / 6);
		}
		
		
		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return normalizeObject(object);
		}
	}),
	
	// Match by generic numeric similarity
	Numeric(new NumericSimilarityMetric() {
		
		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return normalizeObject(object);
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
	
	
	protected static String normalizeObject(Object object) {
		String name = object.toString();
		
		// use name without extension
		if (object instanceof File) {
			name = getName((File) object);
		} else if (object instanceof FileInfo) {
			name = ((FileInfo) object).getName();
		}
		
		// remove checksums, any [...] or (...)
		name = removeEmbeddedChecksum(name);
		
		// remove/normalize special characters
		name = name.replaceAll("['`´]+", "");
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		return name.trim().toLowerCase();
	}
	
	
	public static SimilarityMetric[] defaultSequence(boolean includeFileMetrics) {
		// 1. pass: match by file length (fast, but only works when matching torrents or files)
		// 2. pass: match by season / episode numbers
		// 3. pass: match by checking series / episode title against the file path
		// 4. pass: match by generic name similarity (slow, but most matches will have been determined in second pass)
		// 5. pass: match by generic numeric similarity
		if (includeFileMetrics) {
			return new SimilarityMetric[] { FileSize, new MetricCascade(FileName, EpisodeFunnel), EpisodeBalancer, SubstringFields, Name, Numeric };
		} else {
			return new SimilarityMetric[] { EpisodeFunnel, EpisodeBalancer, SubstringFields, Name, Numeric };
		}
	}
	
	
	public static SimilarityMetric verificationMetric() {
		return new MetricCascade(FileSize, FileName, SeasonEpisode, AirDate, Title, Name);
	}
	
}
