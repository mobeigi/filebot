
package net.sourceforge.filebot;


import net.sourceforge.filebot.format.ExpressionFormatTest;
import net.sourceforge.filebot.hash.VerificationFormatTest;
import net.sourceforge.filebot.media.ReleaseInfoTest;
import net.sourceforge.filebot.similarity.EpisodeMetricsTest;
import net.sourceforge.filebot.similarity.SimilarityTestSuite;
import net.sourceforge.filebot.subtitle.SubtitleReaderTestSuite;
import net.sourceforge.filebot.ui.rename.MatchModelTest;
import net.sourceforge.filebot.web.WebTestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses( { SimilarityTestSuite.class, WebTestSuite.class, ExpressionFormatTest.class, VerificationFormatTest.class, MatchModelTest.class, EpisodeMetricsTest.class, SubtitleReaderTestSuite.class, ReleaseInfoTest.class })
public class FileBotTestSuite {
	
}
