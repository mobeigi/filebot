
package net.sourceforge.filebot.similarity;


import static java.lang.Math.*;


public enum StrictEpisodeMetrics implements SimilarityMetric {
	
	FileName(EpisodeMetrics.FileName, 1), // only allow 0 or 1
	EpisodeIdentifier(EpisodeMetrics.EpisodeIdentifier, 1), // only allow 0 or 1
	SubstringFields(EpisodeMetrics.SubstringFields, 2), // allow 0 or .5 or 1
	Name(EpisodeMetrics.Name, 2); // allow 0 or .5 or 1
	
	// inner metric
	private final SimilarityMetric metric;
	private final float floorFactor;
	

	private StrictEpisodeMetrics(SimilarityMetric metric, float floorFactor) {
		this.metric = metric;
		this.floorFactor = floorFactor;
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return (float) (floor(metric.getSimilarity(o1, o2) * floorFactor) / floorFactor);
	}
	

	public static SimilarityMetric[] defaultSequence(boolean includeFileMetrics) {
		// use SEI for matching and SN for excluding false positives
		if (includeFileMetrics) {
			return new SimilarityMetric[] { FileName, EpisodeIdentifier, SubstringFields, Name };
		} else {
			return new SimilarityMetric[] { EpisodeIdentifier, SubstringFields, Name };
		}
	}
	
}
