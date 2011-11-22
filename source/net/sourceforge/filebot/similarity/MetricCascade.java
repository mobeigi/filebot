
package net.sourceforge.filebot.similarity;


import static java.lang.Math.*;


public class MetricCascade implements SimilarityMetric {
	
	private final SimilarityMetric[] cascade;
	

	public MetricCascade(SimilarityMetric... cascade) {
		this.cascade = cascade;
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		float f = 0;
		for (SimilarityMetric metric : cascade) {
			f = max(f, metric.getSimilarity(o1, o2));
			
			// perfect match, ignore remaining metrics
			if (f >= 1) {
				return f;
			}
		}
		
		return f;
	}
	
}
