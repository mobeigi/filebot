
package net.sourceforge.filebot.ui.panel.rename.metric;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.EuclideanDistance;
import uk.ac.shef.wit.simmetrics.tokenisers.InterfaceTokeniser;
import uk.ac.shef.wit.simmetrics.wordhandlers.DummyStopTermHandler;
import uk.ac.shef.wit.simmetrics.wordhandlers.InterfaceTermHandler;


public class NumericSimilarityMetric extends AbstractNameSimilarityMetric {
	
	private final AbstractStringMetric metric;
	
	
	public NumericSimilarityMetric() {
		// I have absolutely no clue as to why, but I get a good matching behavior  
		// when using my NumberTokensier with EuclideanDistance
		metric = new EuclideanDistance(new NumberTokeniser());
	}
	

	@Override
	public float getSimilarity(String a, String b) {
		return metric.getSimilarity(a, b);
	}
	

	@Override
	public String getDescription() {
		return "Similarity of number patterns";
	}
	

	@Override
	public String getName() {
		return "Numbers";
	}
	
	
	private static class NumberTokeniser implements InterfaceTokeniser {
		
		private final String delimiter = "(\\D)+";
		
		
		@Override
		public ArrayList<String> tokenizeToArrayList(String input) {
			ArrayList<String> tokens = new ArrayList<String>();
			
			Scanner scanner = new Scanner(input);
			scanner.useDelimiter(delimiter);
			
			while (scanner.hasNextInt()) {
				tokens.add(Integer.toString(scanner.nextInt()));
			}
			
			return tokens;
		}
		

		@Override
		public Set<String> tokenizeToSet(String input) {
			return new HashSet<String>(tokenizeToArrayList(input));
		}
		

		@Override
		public String getShortDescriptionString() {
			return getClass().getSimpleName();
		}
		

		@Override
		public String getDelimiters() {
			return delimiter;
		}
		
		private InterfaceTermHandler stopWordHandler = new DummyStopTermHandler();
		
		
		@Override
		public InterfaceTermHandler getStopWordHandler() {
			return stopWordHandler;
		}
		

		@Override
		public void setStopWordHandler(InterfaceTermHandler stopWordHandler) {
			this.stopWordHandler = stopWordHandler;
		}
		
	}
	
}
