
package net.sourceforge.tuned;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { ByteBufferOutputStreamTest.class, PreferencesMapTest.class, PreferencesListTest.class, TreeIteratorTest.class, FilterIteratorTest.class })
public class TunedTestSuite {
	
}
