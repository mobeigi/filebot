
package net.sourceforge.filebot.similarity;


import java.io.File;

import net.sourceforge.filebot.web.Date;


public class DateMetric implements SimilarityMetric {
	
	private final DateMatcher matcher;
	
	
	public DateMetric() {
		this.matcher = new DateMatcher();
	}
	
	
	public DateMetric(DateMatcher matcher) {
		this.matcher = matcher;
	}
	
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		Date d1 = parse(o1);
		if (d1 == null)
			return 0;
		
		Date d2 = parse(o2);
		if (d2 == null)
			return 0;
		
		return d1.equals(d2) ? 1 : -1;
	}
	
	
	public Date parse(Object object) {
		if (object instanceof File) {
			// parse file name
			object = ((File) object).getName();
		}
		
		return matcher.match(object.toString());
	}
	
}
