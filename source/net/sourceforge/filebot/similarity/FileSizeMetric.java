
package net.sourceforge.filebot.similarity;


import java.io.File;


public class FileSizeMetric implements SimilarityMetric {
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		long l1 = getLength(o1);
		
		if (l1 >= 0 && l1 == getLength(o2)) {
			// objects have the same non-negative length
			return 1;
		}
		
		return 0;
	}
	

	protected long getLength(Object o) {
		if (o instanceof File) {
			return ((File) o).length();
		}
		
		return -1;
	}
	
}
