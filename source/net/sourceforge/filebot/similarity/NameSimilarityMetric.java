
package net.sourceforge.filebot.similarity;


import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import uk.ac.shef.wit.simmetrics.tokenisers.TokeniserQGram3;


public class NameSimilarityMetric implements SimilarityMetric {
	
	private final AbstractStringMetric metric;
	

	public NameSimilarityMetric() {
		// QGramsDistance with a QGram tokenizer seems to work best for similarity of names
		metric = new QGramsDistance(new TokeniserQGram3());
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return metric.getSimilarity(normalize(o1), normalize(o2));
	}
	

	protected String normalize(Object object) {
		// use string representation
		String name = object.toString();
		
		// normalize separators
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		// normalize case and trim
		return name.trim().toLowerCase();
	}
	
}
