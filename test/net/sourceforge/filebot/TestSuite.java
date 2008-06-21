
package net.sourceforge.filebot;


import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( {})
public class TestSuite {
	
	public static Test suite() {
		return new JUnit4TestAdapter(TestSuite.class);
	}
	
}
