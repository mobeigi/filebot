
package net.sourceforge.filebot.ui.panel.rename;


import net.sourceforge.filebot.ui.panel.rename.metric.AbstractNameSimilarityMetricTest;
import net.sourceforge.filebot.ui.panel.rename.metric.NumericSimilarityMetricTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { AbstractNameSimilarityMetricTest.class, NumericSimilarityMetricTest.class })
public class MatcherTestSuite {
	
}
