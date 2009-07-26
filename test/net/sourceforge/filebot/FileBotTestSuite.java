
package net.sourceforge.filebot;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.sourceforge.filebot.format.ExpressionFormatTest;
import net.sourceforge.filebot.hash.VerificationFormatTest;
import net.sourceforge.filebot.similarity.SimilarityTestSuite;
import net.sourceforge.filebot.subtitle.SubtitleReaderTestSuite;
import net.sourceforge.filebot.ui.panel.rename.MatchModelTest;
import net.sourceforge.filebot.ui.panel.rename.MatchSimilarityMetricTest;
import net.sourceforge.filebot.web.WebTestSuite;


@RunWith(Suite.class)
@SuiteClasses( { SimilarityTestSuite.class, WebTestSuite.class, ArgumentBeanTest.class, ExpressionFormatTest.class, VerificationFormatTest.class, MatchModelTest.class, MatchSimilarityMetricTest.class, SubtitleReaderTestSuite.class })
public class FileBotTestSuite {
	
}
