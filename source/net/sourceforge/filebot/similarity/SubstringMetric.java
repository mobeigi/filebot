
package net.sourceforge.filebot.similarity;


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
		// use string representation
		String name = object.toString();
		
		// normalize separators
		name = name.replaceAll("['`´]+", "").replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		// normalize case and trim
		return name.trim().toLowerCase();
	}
	
}
