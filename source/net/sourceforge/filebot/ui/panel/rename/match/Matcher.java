
package net.sourceforge.filebot.ui.panel.rename.match;


import java.util.ArrayList;
import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.similarity.SimilarityMetric;


public class Matcher {
	
	public List<Match> match(List<? extends ListEntry<?>> listA, List<? extends ListEntry<?>> listB, SimilarityMetric similarityMetric) {
		List<Match> matches = new ArrayList<Match>();
		
		for (ListEntry<?> entryA : listA) {
			float maxSimilarity = -1;
			ListEntry<?> mostSimilarEntry = null;
			
			for (ListEntry<?> entryB : listB) {
				float similarity = similarityMetric.getSimilarity(entryA, entryB);
				
				if (similarity > maxSimilarity) {
					maxSimilarity = similarity;
					mostSimilarEntry = entryB;
				}
			}
			
			if (mostSimilarEntry != null) {
				listB.remove(mostSimilarEntry);
			}
			
			matches.add(new Match(entryA, mostSimilarEntry));
		}
		
		for (ListEntry<?> entryB : listB) {
			matches.add(new Match(null, entryB));
		}
		
		return matches;
	}
	
}
