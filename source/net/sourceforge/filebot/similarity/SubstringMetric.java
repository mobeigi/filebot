package net.sourceforge.filebot.similarity;

import static net.sourceforge.filebot.similarity.Normalization.normalizePunctuation;

public class SubstringMetric implements SimilarityMetric {

	@Override
	public float getSimilarity(Object o1, Object o2) {
		String s1 = normalize(o1);
		if (s1 == null || s1.isEmpty())
			return 0;

		String s2 = normalize(o2);
		if (s2 == null || s2.isEmpty())
			return 0;

		return s1.contains(s2) || s2.contains(s1) ? 1 : 0;
	}

	protected String normalize(Object object) {
		if (object == null)
			return null;

		// use string representation
		String name = object.toString();

		// normalize separators
		name = normalizePunctuation(name);

		// normalize case and trim
		return name.trim().toLowerCase();
	}

}
