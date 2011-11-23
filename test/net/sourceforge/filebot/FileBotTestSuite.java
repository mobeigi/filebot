
package net.sourceforge.filebot;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.sourceforge.filebot.format.ExpressionFormatTest;
import net.sourceforge.filebot.hash.VerificationFormatTest;
import net.sourceforge.filebot.mediainfo.ReleaseInfoTest;
import net.sourceforge.filebot.similarity.EpisodeMetricsTest;
import net.sourceforge.filebot.similarity.SimilarityTestSuite;
import net.sourceforge.filebot.subtitle.SubtitleReaderTestSuite;
import net.sourceforge.filebot.ui.rename.MatchModelTest;
import net.sourceforge.filebot.web.WebTestSuite;


@RunWith(Suite.class)
@SuiteClasses( { SimilarityTestSuite.class, WebTestSuite.class, ExpressionFormatTest.class, VerificationFormatTest.class, MatchModelTest.class, EpisodeMetricsTest.class, SubtitleReaderTestSuite.class, ReleaseInfoTest.class })
public class FileBotTestSuite {
	
}
