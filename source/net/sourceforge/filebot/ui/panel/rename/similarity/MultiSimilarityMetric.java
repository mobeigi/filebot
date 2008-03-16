
package net.sourceforge.filebot.ui.panel.rename.similarity;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class MultiSimilarityMetric extends SimilarityMetric implements Iterable<SimilarityMetric> {
	
	private List<SimilarityMetric> similarityMetrics;
	
	
	public MultiSimilarityMetric(SimilarityMetric... metrics) {
		similarityMetrics = Arrays.asList(metrics);
	}
	

	@Override
	public float getSimilarity(ListEntry<?> a, ListEntry<?> b) {
		float similarity = 0;
		
		for (SimilarityMetric metric : similarityMetrics) {
			similarity += metric.getSimilarity(a, b) / similarityMetrics.size();
		}
		
		return similarity;
	}
	

	@Override
	public String getDescription() {
		return null;
	}
	

	@Override
	public String getName() {
		return "Average";
	}
	

	@Override
	public Iterator<SimilarityMetric> iterator() {
		return similarityMetrics.iterator();
	}
	
}
