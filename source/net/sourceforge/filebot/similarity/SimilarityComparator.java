
package net.sourceforge.filebot.similarity;


import java.util.Comparator;


public class SimilarityComparator implements Comparator<Object> {
	
	protected SimilarityMetric metric;
	protected Object[] paragon;
	
	
	public SimilarityComparator(SimilarityMetric metric, Object[] paragon) {
		this.metric = metric;
		this.paragon = paragon;
	}
	
	
	public SimilarityComparator(Object... paragon) {
		this(new NameSimilarityMetric(), paragon);
	}
	
	
	@Override
	public int compare(Object o1, Object o2) {
		float f1 = getMaxSimilarity(o1);
		float f2 = getMaxSimilarity(o2);
		
		if (f1 == f2)
			return 0;
		
		return f1 > f2 ? -1 : 1;
	}
	
	
	public float getMaxSimilarity(Object obj) {
		float m = 0;
		for (Object it : paragon) {
			m += (it != null) ? metric.getSimilarity(obj, it) : 0;
		}
		return m / paragon.length;
	}
}
