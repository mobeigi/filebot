
package net.sourceforge.filebot.similarity;


import static net.sourceforge.filebot.FileBotUtilities.*;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;
import uk.ac.shef.wit.simmetrics.tokenisers.TokeniserQGram3Extended;


public class NameSimilarityMetric implements SimilarityMetric {
	
	private final AbstractStringMetric metric;
	
	
	public NameSimilarityMetric() {
		// MongeElkan metric with a QGram3Extended tokenizer seems to work best for similarity of names
		metric = new MongeElkan(new TokeniserQGram3Extended());
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return metric.getSimilarity(normalize(o1), normalize(o2));
	}
	

	protected String normalize(Object object) {
		// remove embedded checksum from name, if any
		String name = removeEmbeddedChecksum(object.toString());
		
		// normalize separators
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		// normalize case and trim
		return name.trim().toLowerCase();
	}
	

	@Override
	public String getDescription() {
		return "Similarity of names";
	}
	

	@Override
	public String getName() {
		return metric.getShortDescriptionString();
	}
	

	@Override
	public String toString() {
		return getClass().getName();
	}
}
