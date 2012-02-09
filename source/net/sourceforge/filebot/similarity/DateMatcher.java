
package net.sourceforge.filebot.similarity;


import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.web.Date;


public class DateMatcher {
	
	private final DatePattern[] patterns;
	
	
	public DateMatcher() {
		patterns = new DatePattern[2];
		
		// match yyyy-mm-dd patterns like 2010-10-24, 2009/6/1, etc.
		patterns[0] = new DatePattern("(?<!\\p{Alnum})(\\d{4})[^\\p{Alnum}](\\d{1,2})[^\\p{Alnum}](\\d{1,2})(?!\\p{Alnum})", new int[] { 1, 2, 3 });
		
		// match dd-mm-yyyy patterns like 1.1.2010, 01/06/2010, etc.
		patterns[1] = new DatePattern("(?<!\\p{Alnum})(\\d{1,2})[^\\p{Alnum}](\\d{1,2})[^\\p{Alnum}](\\d{4})(?!\\p{Alnum})", new int[] { 3, 2, 1 });
	}
	
	
	public DateMatcher(DatePattern... patterns) {
		this.patterns = patterns;
	}
	
	
	public Date match(CharSequence seq) {
		for (DatePattern pattern : patterns) {
			Date match = pattern.match(seq);
			
			if (match != null) {
				return match;
			}
		}
		
		return null;
	}
	
	
	public int find(CharSequence seq, int fromIndex) {
		for (DatePattern pattern : patterns) {
			int pos = pattern.find(seq, fromIndex);
			
			if (pos >= 0) {
				return pos;
			}
		}
		
		return -1;
	}
	
	
	private static class DatePattern {
		
		protected final Pattern pattern;
		protected final int[] order;
		
		
		public DatePattern(String pattern, int[] order) {
			this.pattern = Pattern.compile(pattern);
			this.order = order;
		}
		
		
		protected Date process(MatchResult match) {
			return new Date(Integer.parseInt(match.group(order[0])), Integer.parseInt(match.group(order[1])), Integer.parseInt(match.group(order[2])));
		}
		
		
		public Date match(CharSequence seq) {
			Matcher matcher = pattern.matcher(seq);
			
			if (matcher.find()) {
				return process(matcher);
			}
			
			return null;
		}
		
		
		public int find(CharSequence seq, int fromIndex) {
			Matcher matcher = pattern.matcher(seq).region(fromIndex, seq.length());
			
			if (matcher.find()) {
				return matcher.start();
			}
			
			return -1;
		}
	}
	
}
