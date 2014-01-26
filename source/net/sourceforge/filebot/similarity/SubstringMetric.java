package net.sourceforge.filebot.similarity;

import static net.sourceforge.filebot.similarity.Normalization.*;

public class SubstringMetric implements SimilarityMetric {

	private boolean o1c2;
	private boolean o2c1;

	public SubstringMetric() {
		this(true, true);
	}

	public SubstringMetric(boolean o2c1, boolean o1c2) {
		this.o1c2 = o1c2;
		this.o2c1 = o2c1;
	}

	@Override
	public float getSimilarity(Object o1, Object o2) {
		String s1 = normalize(o1);
		if (s1 == null || s1.isEmpty())
			return 0;

		String s2 = normalize(o2);
		if (s2 == null || s2.isEmpty())
			return 0;

		return (o1c2 && s1.contains(s2)) || (o2c1 && s2.contains(s1)) ? 1 : 0;
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
