
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { net.sourceforge.tuned.TestSuite.class, net.sourceforge.filebot.TestSuite.class })
public class AllTests {
	
	public static Test suite() {
		return new JUnit4TestAdapter(AllTests.class);
	}
	
}
