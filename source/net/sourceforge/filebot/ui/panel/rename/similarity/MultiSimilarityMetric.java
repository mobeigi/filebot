
package net.sourceforge.filebot.ui.panel.rename.similarity;


import java.util.ArrayList;
import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class MultiSimilarityMetric extends SimilarityMetric {
	
	private ArrayList<SimilarityMetric> similarityMetrics = new ArrayList<SimilarityMetric>();
	
	
	@Override
	public float getSimilarity(ListEntry<?> a, ListEntry<?> b) {
		if (similarityMetrics.size() < 1)
			return 0;
		
		float similarity = 0;
		
		for (SimilarityMetric metric : similarityMetrics) {
			similarity += metric.getSimilarity(a, b);
		}
		
		return similarity / similarityMetrics.size();
	}
	

	public void addMetric(SimilarityMetric similarityMetric) {
		similarityMetrics.add(similarityMetric);
	}
	

	public List<SimilarityMetric> getSimilarityMetrics() {
		return similarityMetrics;
	}
	

	@Override
	public String getDescription() {
		return null;
	}
	

	@Override
	public String getName() {
		return "Average";
	}
	
}
