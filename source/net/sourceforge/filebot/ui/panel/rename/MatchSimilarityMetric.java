
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.hash.VerificationUtilities.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.filebot.similarity.DateMetric;
import net.sourceforge.filebot.similarity.FileSizeMetric;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.NumericSimilarityMetric;
import net.sourceforge.filebot.similarity.SeasonEpisodeMetric;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.tuned.FileUtilities;


public enum MatchSimilarityMetric implements SimilarityMetric {
	
	// Match by file length (only works when matching torrents or files)
	FileSize(new FileSizeMetric() {
		
		@Override
		public float getSimilarity(Object o1, Object o2) {
			// order of arguments is logically irrelevant, but we might be able to save us a call to File.length() which is quite costly
			return o1 instanceof File ? super.getSimilarity(o2, o1) : super.getSimilarity(o1, o2);
		}
		

		@Override
		protected long getLength(Object object) {
			if (object instanceof AbstractFile) {
				return ((AbstractFile) object).getLength();
			}
			
			return super.getLength(object);
		}
	}),
	
	// Match by season/episode and airdate combined
	EpisodeIdentifier(new SimilarityMetric() {
		
		@Override
		public float getSimilarity(Object o1, Object o2) {
			float sxeSimilarity = SeasonEpisode.getSimilarity(o1, o2);
			
			// break if SxE is a perfect match already
			if (sxeSimilarity >= 1)
				return sxeSimilarity;
			
			return Math.max(sxeSimilarity, AirDate.getSimilarity(o1, o2));
		}
		
	}),
	
	// Match by season / episode numbers
	SeasonEpisode(new SeasonEpisodeMetric() {
		
		@Override
		protected Collection<SxE> parse(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				
				// get SxE from episode, both SxE for season/episode numbering and SxE for absolute episode numbering
				SxE seasonEpisode = new SxE(episode.getSeason(), episode.getEpisode());
				SxE absoluteEpisode = new SxE(null, episode.getAbsolute());
				
				return seasonEpisode.equals(absoluteEpisode) ? Collections.singleton(absoluteEpisode) : Arrays.asList(seasonEpisode, absoluteEpisode);
			}
			
			return super.parse(object);
		}
	}),
	
	// Match episode airdate
	AirDate(new DateMetric() {
		
		@Override
		protected Date parse(Object object) {
			if (object instanceof Episode) {
				Episode episode = (Episode) object;
				
				// create SxE from episode
				return episode.airdate();
			}
			
			return super.parse(object);
		}
	}),
	
	// Match by generic name similarity
	Name(new NameSimilarityMetric() {
		
		@Override
		public float getSimilarity(Object o1, Object o2) {
			// normalize absolute similarity to similarity rank (10 ranks in total),
			// so we are less likely to fall for false positives in this pass, and move on to the next one
			return (float) (Math.floor(super.getSimilarity(o1, o2) * 10) / 10);
		}
		

		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return super.normalize(normalizeFile(object));
		}
	}),
	
	// Match by generic numeric similarity
	Numeric(new NumericSimilarityMetric() {
		
		@Override
		protected String normalize(Object object) {
			// simplify file name, if possible
			return super.normalize(normalizeFile(object));
		}
	});
	
	// inner metric
	private final SimilarityMetric metric;
	

	private MatchSimilarityMetric(SimilarityMetric metric) {
		this.metric = metric;
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return metric.getSimilarity(o1, o2);
	}
	

	protected static String normalizeFile(Object object) {
		String name = object.toString();
		
		// use name without extension
		if (object instanceof File) {
			name = FileUtilities.getName((File) object);
		} else if (object instanceof AbstractFile) {
			name = FileUtilities.getNameWithoutExtension(((AbstractFile) object).getName());
		}
		
		// remove embedded checksum from name, if any
		return removeEmbeddedChecksum(name);
	}
	

	public static SimilarityMetric[] defaultSequence() {
		// 1. pass: match by file length (fast, but only works when matching torrents or files)
		// 2. pass: match by season / episode numbers
		// 3. pass: match by generic name similarity (slow, but most matches will have been determined in second pass)
		// 4. pass: match by generic numeric similarity
		return new SimilarityMetric[] { FileSize, EpisodeIdentifier, Name, Numeric };
	}
	
}
