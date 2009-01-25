
package net.sourceforge.filebot.similarity;


import static net.sourceforge.filebot.FileBotUtilities.removeEmbeddedChecksum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import uk.ac.shef.wit.simmetrics.tokenisers.InterfaceTokeniser;
import uk.ac.shef.wit.simmetrics.wordhandlers.DummyStopTermHandler;
import uk.ac.shef.wit.simmetrics.wordhandlers.InterfaceTermHandler;


public class NumericSimilarityMetric implements SimilarityMetric {
	
	private final AbstractStringMetric metric;
	
	
	public NumericSimilarityMetric() {
		// I don't really know why, but I get a good matching behavior 
		// when using QGramsDistance or BlockDistance
		metric = new QGramsDistance(new NumberTokeniser());
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return metric.getSimilarity(normalize(o1), normalize(o2));
	}
	

	protected String normalize(Object object) {
		// delete checksum pattern, because it will mess with the number tokens
		return removeEmbeddedChecksum(object.toString());
	}
	

	@Override
	public String getDescription() {
		return "Similarity of number patterns";
	}
	

	@Override
	public String getName() {
		return "Numbers";
	}
	

	@Override
	public String toString() {
		return getClass().getName();
	}
	
	
	protected static class NumberTokeniser implements InterfaceTokeniser {
		
		private final String delimiter = "\\D+";
		
		
		@Override
		public ArrayList<String> tokenizeToArrayList(String input) {
			ArrayList<String> tokens = new ArrayList<String>();
			
			Scanner scanner = new Scanner(input);
			
			// scan for number patterns, use non-number pattern as delimiter
			scanner.useDelimiter(delimiter);
			
			while (scanner.hasNextInt()) {
				// remove leading zeros from number tokens by scanning for Integers
				tokens.add(String.valueOf(scanner.nextInt()));
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
