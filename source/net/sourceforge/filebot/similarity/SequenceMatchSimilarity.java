
package net.sourceforge.filebot.similarity;


import static java.lang.Math.*;
import static net.sourceforge.filebot.similarity.Normalization.*;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;


public class SequenceMatchSimilarity implements SimilarityMetric {
	
	@Override
	public float getSimilarity(Object o1, Object o2) {
		String s1 = normalize(o1);
		String s2 = normalize(o2);
		
		// match common word sequence
		String match = match(s1, s2);
		if (match == null)
			return 0;
		
		return (float) match.length() / min(s1.length(), s2.length());
	}
	
	
	protected String normalize(Object object) {
		// use string representation
		String name = object.toString();
		
		// normalize separators
		name = normalizePunctuation(name);
		
		// normalize case and trim
		return name.trim().toLowerCase();
	}
	
	
	protected String match(String s1, String s2) {
		// use maximum strength collator by default
		Collator collator = Collator.getInstance(Locale.ROOT);
		collator.setDecomposition(Collator.FULL_DECOMPOSITION);
		collator.setStrength(Collator.TERTIARY);
		
		@SuppressWarnings("unchecked")
		SeriesNameMatcher matcher = new SeriesNameMatcher((Comparator) collator, 10) {
			
			@Override
			protected String normalize(String name) {
				return name; // assume normalization has been done, no need to do that here again
			};
		};
		
		return matcher.matchByFirstCommonWordSequence(s1, s2);
	}
}
