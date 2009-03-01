
package net.sourceforge.filebot.similarity;


import java.io.File;
import java.util.Collection;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;


public class SeasonEpisodeSimilarityMetric implements SimilarityMetric {
	
	private final SeasonEpisodeMatcher seasonEpisodeMatcher = new SeasonEpisodeMatcher();
	
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		Collection<SxE> sxeVector1 = parse(o1);
		Collection<SxE> sxeVector2 = parse(o2);
		
		if (sxeVector1 == null || sxeVector2 == null) {
			// name does not match any known pattern, return numeric similarity
			return 0;
		}
		
		float similarity = 0;
		
		for (SxE sxe1 : sxeVector1) {
			for (SxE sxe2 : sxeVector2) {
				if (sxe1.episode == sxe2.episode) {
					if (sxe1.season == sxe2.season) {
						// vectors have at least one perfect episode match in common
						return 1;
					}
					
					// at least we have a partial match
					similarity = 0.5f;
				}
			}
		}
		
		return similarity;
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
