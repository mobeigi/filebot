
package net.sourceforge.filebot.ui.panel.rename.metric;


import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;
import uk.ac.shef.wit.simmetrics.tokenisers.TokeniserQGram3Extended;


public class StringSimilarityMetric extends AbstractNameSimilarityMetric {
	
	private final AbstractStringMetric metric;
	
	
	public StringSimilarityMetric() {
		// I have absolutely no clue as to why, but I get a good matching behavior  
		// when using MongeElkan with a QGram3Extended (far from perfect though)
		metric = new MongeElkan(new TokeniserQGram3Extended());
		
		//TODO QGram3Extended VS Whitespace (-> normalized values)
	}
	

	@Override
	public float getSimilarity(String a, String b) {
		return metric.getSimilarity(a, b);
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
