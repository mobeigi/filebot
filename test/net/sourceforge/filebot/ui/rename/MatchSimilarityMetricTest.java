
package net.sourceforge.filebot.ui.rename;


import static net.sourceforge.filebot.ui.rename.MatchSimilarityMetric.*;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;


public class MatchSimilarityMetricTest {
	
	@Test
	public void substringMetrics() {
		Episode eY1T1 = new Episode("Doctor Who", new Date(2005, 0, 0), 1, 1, "Rose");
		Episode eY2T2 = new Episode("Doctor Who", new Date(1963, 0, 0), 1, 1, "An Unearthly Child");
		File fY1T1 = new File("Doctor Who (2005)/Doctor Who - 1x01 - Rose");
		File fY2T2 = new File("Doctor Who (1963)/Doctor Who - 1x01 - An Unearthly Child");
		
		assertEquals(3.0 / 3, Title.getSimilarity(eY1T1, fY1T1), 0);
		assertEquals(2.0 / 3, Title.getSimilarity(eY1T1, fY2T2), 0.01);
	}
	

	@Test
	public void nameIgnoreEmbeddedChecksum() {
		assertEquals(1, Name.getSimilarity("test", "test [EF62DF13]"), 0);
	}
	

	@Test
	public void numericIgnoreEmbeddedChecksum() {
		assertEquals(1, Numeric.getSimilarity("S01E02", "Season 1, Episode 2 [00A01E02]"), 0);
	}
	

	@Test
	public void normalizeFile() {
		assertEquals("abc", MatchSimilarityMetric.normalizeObject(new File("/folder/abc[EF62DF13].txt")));
	}
	
}
