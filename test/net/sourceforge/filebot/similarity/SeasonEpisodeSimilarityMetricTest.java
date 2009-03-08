
package net.sourceforge.filebot.similarity;


import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class SeasonEpisodeSimilarityMetricTest {
	
	private static SeasonEpisodeSimilarityMetric metric = new SeasonEpisodeSimilarityMetric();
	
	
	@Test
	public void getSimilarity() {
		// single pattern match, single episode match
		assertEquals(1.0, metric.getSimilarity("1x01", "s01e01"), 0);
		
		// multiple pattern matches, single episode match
		assertEquals(1.0, metric.getSimilarity("1x02a", "101 102 103"), 0);
		
		// multiple pattern matches, no episode match
		assertEquals(0.0, metric.getSimilarity("1x03b", "104 105 106"), 0);
		
		// no pattern match, no episode match
		assertEquals(0.0, metric.getSimilarity("abc", "xyz"), 0);
	}
	
}
