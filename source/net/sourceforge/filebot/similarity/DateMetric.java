
package net.sourceforge.filebot.similarity;


import java.io.File;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.web.Date;


public class DateMetric implements SimilarityMetric {
	
	private final DatePattern[] patterns;
	

	public DateMetric() {
		patterns = new DatePattern[2];
		
		// match yyyy-mm-dd patterns like 2010-10-24, 2009/6/1, etc.
		patterns[0] = new DatePattern("(?<!\\p{Alnum})(\\d{4})[^\\p{Alnum}](\\d{1,2})[^\\p{Alnum}](\\d{1,2})(?!\\p{Alnum})", new int[] { 1, 2, 3 });
		
		// match dd-mm-yyyy patterns like 1.1.2010, 01/06/2010, etc.
		patterns[1] = new DatePattern("(?<!\\p{Alnum})(\\d{1,2})[^\\p{Alnum}](\\d{1,2})[^\\p{Alnum}](\\d{4})(?!\\p{Alnum})", new int[] { 3, 2, 1 });
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		Date d1 = parse(o1);
		Date d2 = parse(o2);
		
		return d1 != null && d2 != null && d1.equals(d2) ? 1 : 0;
	}
	

	protected Date parse(Object object) {
		if (object instanceof File) {
			// parse file name
			object = ((File) object).getName();
		}
		
		return match(object.toString());
	}
	

	protected Date match(CharSequence name) {
		for (DatePattern pattern : patterns) {
			Date match = pattern.match(name);
			
			if (match != null) {
				return match;
			}
		}
		
		return null;
	}
	

	protected static class DatePattern {
		
		protected final Pattern pattern;
		protected final int[] order;
		

		public DatePattern(String pattern, int[] order) {
			this.pattern = Pattern.compile(pattern);
			this.order = order;
		}
		

		protected Date process(MatchResult match) {
			return new Date(Integer.parseInt(match.group(order[0])), Integer.parseInt(match.group(order[1])), Integer.parseInt(match.group(order[2])));
		}
		

		public Date match(CharSequence name) {
			Matcher matcher = pattern.matcher(name);
			
			if (matcher.find()) {
				return process(matcher);
			}
			
			return null;
		}
	}
	
}
