
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.ui.panel.rename.MatchSimilarityMetric.*;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;


public class MatchSimilarityMetricTest {
	
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
		assertEquals("abc", MatchSimilarityMetric.normalizeFile(new File("/folder/abc[EF62DF13].txt")));
	}
	
}
