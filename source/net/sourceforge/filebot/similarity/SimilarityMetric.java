
package net.sourceforge.filebot.similarity;


public interface SimilarityMetric {
	
	public float getSimilarity(Object o1, Object o2);
	

	public String getDescription();
	

	public String getName();
	
}
