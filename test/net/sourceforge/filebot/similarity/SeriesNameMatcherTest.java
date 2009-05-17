
package net.sourceforge.filebot.similarity;


import static org.junit.Assert.*;
import net.sourceforge.filebot.similarity.SeriesNameMatcher.SeriesNameCollection;

import org.junit.Test;


public class SeriesNameMatcherTest {
	
	private static SeriesNameMatcher matcher = new SeriesNameMatcher();
	
	
	@Test
	public void match() {
		assertEquals("Test Series", matcher.match("My Test Series - 1x01", "Test Series - Season 1"));
	}
	

	@Test
	public void matchBeforeSeasonEpisodePattern() {
		assertEquals("The Test", matcher.matchBySeasonEpisodePattern("The Test - 1x01"));
		
		// real world test
		assertEquals("Mushishi", matcher.matchBySeasonEpisodePattern("[niizk]_Mushishi_-_01_-_The_Green_Gathering"));
	}
	

	@Test
	public void normalize() {
		// non-letter and non-digit characters
		assertEquals("The Test", matcher.normalize("_The_Test_-_ ..."));
		
		// brackets
		assertEquals("Luffy", matcher.normalize("[strawhat] Luffy [D.] [#Monkey]"));
		
		// invalid brackets
		assertEquals("strawhat Luffy", matcher.normalize("(strawhat [Luffy (#Monkey)"));
	}
	

	@Test
	public void firstCommonSequence() {
		String[] seq1 = "[abc] Common Name 1".split("\\s");
		String[] seq2 = "[xyz] Common Name 2".split("\\s");
		
		assertArrayEquals(new String[] { "Common", "Name" }, matcher.firstCommonSequence(seq1, seq2, String.CASE_INSENSITIVE_ORDER));
	}
	

	@Test
	public void firstCharacterCaseBalance() {
		SeriesNameCollection n = new SeriesNameCollection();
		
		assertTrue(n.firstCharacterCaseBalance("My Name is Earl") > n.firstCharacterCaseBalance("My Name Is Earl"));
		assertTrue(n.firstCharacterCaseBalance("My Name is Earl") > n.firstCharacterCaseBalance("my name is earl"));
		
		// boost upper case ration
		assertTrue(n.firstCharacterCaseBalance("Roswell") > n.firstCharacterCaseBalance("roswell"));
		
	}
}
