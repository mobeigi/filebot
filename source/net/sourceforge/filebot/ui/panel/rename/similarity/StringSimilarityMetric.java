
package net.sourceforge.filebot.ui.panel.rename.similarity;


import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;


public class StringSimilarityMetric extends SimilarityMetric {
	
	private AbstractStringMetric metric;
	
	
	public StringSimilarityMetric() {
		this(new MongeElkan());
	}
	

	public StringSimilarityMetric(AbstractStringMetric metric) {
		this.metric = metric;
	}
	

	@Override
	public float getSimilarity(ListEntry<?> a, ListEntry<?> b) {
		return metric.getSimilarity(a.toString(), b.toString());
	}
	

	@Override
	public String getDescription() {
		return "Similarity of names";
	}
	

	@Override
	public String getName() {
		return metric.getShortDescriptionString();
	}
	
}
