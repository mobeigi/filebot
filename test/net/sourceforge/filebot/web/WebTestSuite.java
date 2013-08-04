package net.sourceforge.filebot.web;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AnidbClientTest.class, TVRageClientTest.class, TheTVDBClientTest.class, SerienjunkiesClientTest.class, TMDbClientTest.class, IMDbClientTest.class, OpenSubtitlesXmlRpcTest.class })
public class WebTestSuite {

}
