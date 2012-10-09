
package net.sourceforge.filebot.similarity;


import static java.lang.Math.*;

import java.io.File;


public class TimeStampMetric implements SimilarityMetric {
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		long t1 = getTimeStamp(o1);
		long t2 = getTimeStamp(o2);
		
		if (t1 <= 0 || t2 <= 0)
			return 0;
		
		float min = min(t1, t2);
		float max = max(t1, t2);
		
		return min / max;
	}
	
	
	public long getTimeStamp(Object obj) {
		if (obj instanceof File) {
			return ((File) obj).lastModified();
		}
		if (obj instanceof Number) {
			return ((Number) obj).longValue();
		}
		
		return -1;
	}
	
}
