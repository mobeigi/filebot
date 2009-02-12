
package net.sourceforge.filebot.similarity;


import java.io.File;
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;


public class SeasonEpisodeSimilarityMetric implements SimilarityMetric {
	
	private final NumericSimilarityMetric fallbackMetric = new NumericSimilarityMetric();
	
	private final SeasonEpisodeMatcher seasonEpisodeMatcher = new SeasonEpisodeMatcher();
	
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		Collection<SxE> sxeVector1 = parse(o1);
		Collection<SxE> sxeVector2 = parse(o2);
		
		if (sxeVector1 == null || sxeVector2 == null) {
			// name does not match any known pattern, return numeric similarity
			return fallbackMetric.getSimilarity(o1, o2);
		}
		
		if (Collections.disjoint(sxeVector1, sxeVector2)) {
			// vectors have no episode matches in common 
			return 0;
		}
		
		// vectors have at least one episode match in common
		return 1;
	}
	

	protected Collection<SxE> parse(Object object) {
		if (object instanceof File) {
			// parse file name
			object = ((File) object).getName();
		}
		
		return seasonEpisodeMatcher.match(object.toString());
	}
	

	@Override
	public String getDescription() {
		return "Similarity of season and episode numbers";
	}
	

	@Override
	public String getName() {
		return "Season and Episode";
	}
	

	@Override
	public String toString() {
		return getClass().getName();
	}
	
}
