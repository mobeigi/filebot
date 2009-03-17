
package net.sourceforge.filebot.web;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { TVDotComClientTest.class, AnidbClientTest.class, TVRageClientTest.class, TheTVDBClientTest.class, IMDbClientTest.class, SubsceneSubtitleClientTest.class, SubtitleSourceClientTest.class, OpenSubtitlesHasherTest.class })
public class WebTestSuite {
	
}
