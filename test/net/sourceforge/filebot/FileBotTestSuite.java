
package net.sourceforge.filebot;


import net.sourceforge.filebot.similarity.SimilarityTestSuite;
import net.sourceforge.filebot.web.WebTestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { SimilarityTestSuite.class, WebTestSuite.class, ArgumentBeanTest.class })
public class FileBotTestSuite {
	
}
