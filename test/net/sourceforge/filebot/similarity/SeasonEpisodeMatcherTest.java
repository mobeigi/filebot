
package net.sourceforge.filebot.similarity;


import static net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE.UNDEFINED;
import static org.junit.Assert.assertEquals;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;

import org.junit.Test;


public class SeasonEpisodeMatcherTest {
	
	private static SeasonEpisodeMatcher matcher = new SeasonEpisodeMatcher();
	
	
	@Test
	public void patternPrecedence() {
		// S01E01 pattern has highest precedence
		assertEquals(new SxE(1, 3), matcher.match("Test.101.1x02.S01E03").get(0));
		
		// multiple values
		assertEquals(new SxE(1, 2), matcher.match("Test.42.s01e01.s01e02.300").get(1));
	}
	

	@Test
	public void pattern_1x01() {
		assertEquals(new SxE(1, 1), matcher.match("1x01").get(0));
		
		// test multiple matches
		assertEquals(new SxE(1, 2), matcher.match("Test - 1x01 and 1.02 - Multiple MatchCollection").get(1));
		
		// test high values
		assertEquals(new SxE(12, 345), matcher.match("Test - 12x345 - High Values").get(0));
		
		// test lookahead and lookbehind
		assertEquals(new SxE(1, 3), matcher.match("Test_-_103_[1280x720]").get(0));
	}
	

	@Test
	public void pattern_S01E01() {
		assertEquals(new SxE(1, 1), matcher.match("S01E01").get(0));
		
		// test multiple matches
		assertEquals(new SxE(1, 2), matcher.match("S01E01 and S01E02 - Multiple MatchCollection").get(1));
		
		// test separated values
		assertEquals(new SxE(1, 3), matcher.match("[s01]_[e03]").get(0));
		
		// test high values
		assertEquals(new SxE(12, 345), matcher.match("Test - S12E345 - High Values").get(0));
	}
	

	@Test
	public void pattern_101() {
		assertEquals(new SxE(1, 1), matcher.match("Test.101").get(0));
		
		// test 2-digit number
		assertEquals(new SxE(UNDEFINED, 2), matcher.match("02").get(0));
		
		// test high values
		assertEquals(new SxE(10, 1), matcher.match("[Test]_1001_High_Values").get(0));
		
		// first two digits <= 29
		assertEquals(null, matcher.match("The 4400"));
		
		// test lookbehind
		assertEquals(null, matcher.match("720p"));
	}
	
}
