
package net.sourceforge.filebot.similarity;


import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class SeasonEpisodeSimilarityMetricTest {
	
	private static SeasonEpisodeSimilarityMetric metric = new SeasonEpisodeSimilarityMetric();
	
	
	@Test
	public void getSimilarity() {
		// single pattern match, single episode match
		assertEquals(1.0, metric.getSimilarity("1x01", "s01e01"));
		
		// multiple pattern matches, single episode match
		assertEquals(1.0, metric.getSimilarity("1x02a", "101 102 103"));
		
		// multiple pattern matches, no episode match
		assertEquals(0.0, metric.getSimilarity("1x03b", "104 105 106"));
		
		// no pattern match, no episode match
		assertEquals(0.0, metric.getSimilarity("abc", "xyz"));
	}
	

	@Test
	public void fallbackMetric() {
		assertEquals(1.0, metric.getSimilarity("1x01", "sno=1, eno=1"));
		
		assertEquals(1.0, metric.getSimilarity("1x02", "Dexter - Staffel 1 Episode 2"));
	}
	

	@Test
	public void patternPrecedence() {
		// S01E01 pattern has highest precedence
		assertEquals("1x03", metric.match("Test.101.1x02.S01E03").get(0).toString());
		
		// multiple values
		assertEquals("1x02", metric.match("Test.42.s01e01.s01e02.300").get(1).toString());
	}
	

	@Test
	public void pattern_1x01() {
		assertEquals("1x01", metric.match("1x01").get(0).toString());
		
		// test multiple matches
		assertEquals("1x02", metric.match("Test - 1x01 and 1x02 - Multiple MatchCollection").get(1).toString());
		
		// test high values
		assertEquals("12x345", metric.match("Test - 12x345 - High Values").get(0).toString());
		
		// test lookahead and lookbehind
		assertEquals("1x03", metric.match("Test_-_103_[1280x720]").get(0).toString());
	}
	

	@Test
	public void pattern_S01E01() {
		assertEquals("1x01", metric.match("S01E01").get(0).toString());
		
		// test multiple matches
		assertEquals("1x02", metric.match("S01E01 and S01E02 - Multiple MatchCollection").get(1).toString());
		
		// test separated values
		assertEquals("1x03", metric.match("[s01]_[e03]").get(0).toString());
		
		// test high values
		assertEquals("12x345", metric.match("Test - S12E345 - High Values").get(0).toString());
	}
	

	@Test
	public void pattern_101() {
		assertEquals("1x01", metric.match("Test.101").get(0).toString());
		
		// test 2-digit number
		assertEquals("0x02", metric.match("02").get(0).toString());
		
		// test high values
		assertEquals("10x01", metric.match("[Test]_1001_High_Values").get(0).toString());
		
		// first two digits <= 29
		assertEquals(null, metric.match("The 4400"));
	}
	
}
