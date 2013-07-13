
package net.sourceforge.filebot.similarity;


import static net.sourceforge.filebot.similarity.EpisodeMetrics.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;

import org.junit.Test;


public class EpisodeMetricsTest {
	
	@Test
	public void substringMetrics() {
		Episode eY1T1 = new Episode("Doctor Who", new Date(2005, 0, 0), 1, 1, "Rose", null);
		// Episode eY2T2 = new Episode("Doctor Who", new Date(1963, 0, 0), 1, 1, "An Unearthly Child");
		File fY1T1 = new File("Doctor Who (2005)/Doctor Who - 1x01 - Rose");
		File fY2T2 = new File("Doctor Who (1963)/Doctor Who - 1x01 - An Unearthly Child");
		
		assertEquals(3.0 / 3, SubstringFields.getSimilarity(eY1T1, fY1T1), 0);
		assertEquals(2.0 / 3, SubstringFields.getSimilarity(eY1T1, fY2T2), 0.01);
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
		assertEquals("abc", EpisodeMetrics.normalizeObject(new File("/folder/abc[EF62DF13].txt")));
	}
	
	
	@Test
	public void matcherLevel2() throws Exception {
		List<File> files = new ArrayList<File>();
		List<Episode> episodes = new ArrayList<Episode>();
		
		files.add(new File("Greek/Greek - S01E19 - No Campus for Old Rules"));
		files.add(new File("Veronica Mars - Season 1/Veronica Mars [1x19] Hot Dogs"));
		episodes.add(new Episode("Veronica Mars", null, 1, 19, "Hot Dogs", null));
		episodes.add(new Episode("Greek", null, 1, 19, "No Campus for Old Rules", null));
		
		SimilarityMetric[] metrics = new SimilarityMetric[] { EpisodeIdentifier, SubstringFields };
		List<Match<File, Episode>> m = new Matcher<File, Episode>(files, episodes, true, metrics).match();
		
		assertEquals("Greek - S01E19 - No Campus for Old Rules", m.get(0).getValue().getName());
		assertEquals("Greek - 1x19 - No Campus for Old Rules", m.get(0).getCandidate().toString());
		assertEquals("Veronica Mars [1x19] Hot Dogs", m.get(1).getValue().getName());
		assertEquals("Veronica Mars - 1x19 - Hot Dogs", m.get(1).getCandidate().toString());
	}
	
}
