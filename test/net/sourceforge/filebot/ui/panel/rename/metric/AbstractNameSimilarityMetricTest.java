
package net.sourceforge.filebot.ui.panel.rename.metric;


import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.tuned.TestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class AbstractNameSimilarityMetricTest {
	
	private static final BasicNameSimilarityMetric metric = new BasicNameSimilarityMetric();
	
	
	@Parameters
	public static Collection<Object[]> createParameters() {
		Map<String, String> matches = new LinkedHashMap<String, String>();
		
		// normalize separators
		matches.put("test s01e01 first", "test.S01E01.First");
		matches.put("test s01e02 second", "test_S01E02_Second");
		matches.put("test s01e03 third", "__test__S01E03__Third__");
		matches.put("test s01e04 four", "test   s01e04     four");
		
		// strip checksum
		matches.put("test", "test [EF62DF13]");
		
		// lower-case
		matches.put("the a-team", "The A-Team");
		
		return TestUtil.asParameters(matches.entrySet().toArray());
	}
	
	private Entry<String, String> entry;
	
	
	public AbstractNameSimilarityMetricTest(Entry<String, String> entry) {
		this.entry = entry;
	}
	

	@Test
	public void normalize() {
		String normalizedName = entry.getKey();
		String unnormalizedName = entry.getValue();
		
		assertEquals(normalizedName, metric.normalize(unnormalizedName));
	}
	
	
	private static class BasicNameSimilarityMetric extends AbstractNameSimilarityMetric {
		
		@Override
		public float getSimilarity(String a, String b) {
			return a.equals(b) ? 1 : 0;
		}
		

		@Override
		public String getDescription() {
			return "Equals";
		}
		

		@Override
		public String getName() {
			return "Equals";
		}
		
	}
	
}
