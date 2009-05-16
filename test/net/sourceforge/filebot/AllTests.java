
package net.sourceforge.filebot;


import net.sourceforge.tuned.TunedTestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { FileBotTestSuite.class, TunedTestSuite.class })
public class AllTests {
	
}
