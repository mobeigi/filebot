
package net.sourceforge.filebot;


import net.sourceforge.filebot.format.ExpressionFormatTest;
import net.sourceforge.filebot.similarity.SimilarityTestSuite;
import net.sourceforge.filebot.ui.panel.rename.MatchModelTest;
import net.sourceforge.filebot.ui.panel.sfv.VerificationFileScannerTest;
import net.sourceforge.filebot.web.WebTestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { SimilarityTestSuite.class, WebTestSuite.class, ArgumentBeanTest.class, ExpressionFormatTest.class, VerificationFileScannerTest.class, MatchModelTest.class })
public class FileBotTestSuite {
	
}
