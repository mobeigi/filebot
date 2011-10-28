
package net.sourceforge.filebot.similarity;


public class SubstringMetric implements SimilarityMetric {
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		String s1 = normalize(o1);
		String s2 = normalize(o2);
		String pri = s1.length() > s2.length() ? s1 : s2;
		String sub = s1.length() > s2.length() ? s2 : s1;
		
		return sub.length() > 0 && pri.contains(sub) ? 1 : 0;
	}
	

	protected String normalize(Object object) {
		// use string representation
		String name = object.toString();
		
		// normalize separators
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		// normalize case and trim
		return name.trim().toLowerCase();
	}
	
}
