package net.sourceforge.filebot.similarity;

import static java.lang.Math.*;

public class MetricCascade implements SimilarityMetric {

	private final SimilarityMetric[] cascade;
	private final boolean shortCircuit;

	public MetricCascade(SimilarityMetric... cascade) {
		this(true, cascade);
	}

	public MetricCascade(boolean shortCircuit, SimilarityMetric... cascade) {
		this.cascade = cascade;
		this.shortCircuit = shortCircuit;
	}

	@Override
	public float getSimilarity(Object o1, Object o2) {
		float f = 0;
		for (SimilarityMetric metric : cascade) {
			float similarity = metric.getSimilarity(o1, o2);
			if (abs(similarity) >= abs(f)) {
				// perfect match, ignore remaining metrics
				if (shortCircuit) {
					if (similarity >= 1) {
						return similarity;
					} else if (similarity <= -1) {
						return similarity;
					}
				}

				// possible match or perfect negative match
				f = similarity;
			}
		}

		return f;
	}

}
