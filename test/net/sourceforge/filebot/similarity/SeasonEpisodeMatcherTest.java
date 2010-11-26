
package net.sourceforge.filebot.similarity;


import static java.util.Arrays.*;
import static net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE.*;
import static org.junit.Assert.*;

import org.junit.Test;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;


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
		
		// test look-ahead and look-behind
		assertEquals(new SxE(1, 3), matcher.match("Test_-_103_[1280x720]").get(0));
	}
	

	@Test
	public void pattern_S01E01() {
		assertEquals(new SxE(1, 1), matcher.match("S01E01").get(0));
		assertEquals(new SxE(2010, 0), matcher.match("S2010E00").get(0));
		
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
		
		// test season digits <= 19
		assertEquals(null, matcher.match("The 4400"));
		
		// test look-behind
		assertEquals(null, matcher.match("720p"));
		
		// test ambiguous match processing
		assertEquals(asList(new SxE(1, 1), new SxE(UNDEFINED, 101)), matcher.match("Test.101"));
	}
	
}
