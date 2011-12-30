
package net.sourceforge.filebot.similarity;


import java.util.Comparator;


public class SimilarityComparator implements Comparator<Object> {
	
	private SimilarityMetric metric;
	private Object paragon;
	
	
	public SimilarityComparator(SimilarityMetric metric, Object paragon) {
		this.metric = metric;
		this.paragon = paragon;
	}
	
	
	public SimilarityComparator(String paragon) {
		this(new NameSimilarityMetric(), paragon);
	}
	
	
	@Override
	public int compare(Object o1, Object o2) {
		float f1 = metric.getSimilarity(o1, paragon);
		float f2 = metric.getSimilarity(o2, paragon);
		
		if (f1 == f2)
			return 0;
		
		return f1 > f2 ? -1 : 1;
	}
	
}
