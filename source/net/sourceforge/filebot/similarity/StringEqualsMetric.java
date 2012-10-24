
package net.sourceforge.filebot.similarity;


public class StringEqualsMetric implements SimilarityMetric {
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		if (o1 == null || o2 == null)
			return 0;
		
		String s1 = o1.toString();
		String s2 = o2.toString();
		
		if (s1.isEmpty() || s2.isEmpty())
			return 0;
		
		return s1.equalsIgnoreCase(s2) ? 1 : 0;
	}
	
}
