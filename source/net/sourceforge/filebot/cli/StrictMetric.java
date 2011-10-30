
package net.sourceforge.filebot.cli;


import static java.lang.Math.*;

import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.rename.MatchSimilarityMetric;


public enum StrictMetric implements SimilarityMetric {
	
	EpisodeIdentifier(MatchSimilarityMetric.EpisodeIdentifier, 1), // only allow 0 or 1
	Title(MatchSimilarityMetric.Title, 2), // allow 0 or .5 or 1
	Name(MatchSimilarityMetric.Name, 2); // allow 0 or .5 or 1
	
	// inner metric
	private final SimilarityMetric metric;
	private final float floorFactor;
	

	private StrictMetric(SimilarityMetric metric, float floorFactor) {
		this.metric = metric;
		this.floorFactor = floorFactor;
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return (float) (floor(metric.getSimilarity(o1, o2) * floorFactor) / floorFactor);
	}
	
}
