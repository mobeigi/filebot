package net.filebot;

import net.filebot.format.ExpressionFormatTest;
import net.filebot.hash.VerificationFormatTest;
import net.filebot.media.MediaDetectionTest;
import net.filebot.media.ReleaseInfoTest;
import net.filebot.similarity.EpisodeMetricsTest;
import net.filebot.similarity.SimilarityTestSuite;
import net.filebot.subtitle.SubtitleReaderTestSuite;
import net.filebot.ui.rename.MatchModelTest;
import net.filebot.util.UtilTestSuite;
import net.filebot.web.WebTestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ SimilarityTestSuite.class, WebTestSuite.class, ExpressionFormatTest.class, VerificationFormatTest.class, MatchModelTest.class, EpisodeMetricsTest.class, SubtitleReaderTestSuite.class, ReleaseInfoTest.class, MediaDetectionTest.class, UtilTestSuite.class })
public class AllTests {

}
