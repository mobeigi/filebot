package net.filebot.web;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AnidbClientTest.class, TVRageClientTest.class, TheTVDBClientTest.class, TMDbClientTest.class, IMDbClientTest.class, OpenSubtitlesXmlRpcTest.class, AcoustIDClientTest.class })
public class WebTestSuite {

}
