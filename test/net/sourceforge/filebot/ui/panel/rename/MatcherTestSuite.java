
package net.sourceforge.filebot.ui.panel.rename;


import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import net.sourceforge.filebot.ui.panel.rename.metric.AbstractNameSimilarityMetricTest;
import net.sourceforge.filebot.ui.panel.rename.metric.NumericSimilarityMetricTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { AbstractNameSimilarityMetricTest.class, NumericSimilarityMetricTest.class })
public class MatcherTestSuite {
	
	public static Test suite() {
		return new JUnit4TestAdapter(MatcherTestSuite.class);
	}
	
}
