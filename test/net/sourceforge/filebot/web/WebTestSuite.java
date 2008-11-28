
package net.sourceforge.filebot.web;


import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { TVDotComClientTest.class, AnidbClientTest.class, TVRageClientTest.class, SubsceneSubtitleClientTest.class, OpenSubtitlesHasherTest.class })
public class WebTestSuite {
	
	public static Test suite() {
		return new JUnit4TestAdapter(WebTestSuite.class);
	}
	
}
