
package net.sourceforge.filebot;


import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import net.sourceforge.filebot.ui.panel.rename.MatcherTestSuite;
import net.sourceforge.filebot.web.WebTestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { MatcherTestSuite.class, WebTestSuite.class, ArgumentBeanTest.class })
public class FileBotTestSuite {
	
	public static Test suite() {
		return new JUnit4TestAdapter(FileBotTestSuite.class);
	}
	
}
