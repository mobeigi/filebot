
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import net.sourceforge.filebot.ui.panel.subtitle.LanguageResolver;
import net.sourceforge.filebot.web.SubsceneSubtitleClient.SubsceneSearchResult;

import org.junit.BeforeClass;
import org.junit.Test;


public class SubsceneSubtitleClientTest {
	
	private static SubsceneSearchResult testResult;
	private static SubsceneSearchResult manySubtitlesTestResult;
	
	private SubsceneSubtitleClient subscene = new SubsceneSubtitleClient();
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testResult = new SubsceneSearchResult("Twin Peaks - First Season (1990)", new URL("http://subscene.com/twin-peaks--first-season/subtitles-32482.aspx"), 17);
		manySubtitlesTestResult = new SubsceneSearchResult("Lost - Fourth Season (2008)", new URL("http://subscene.com/Lost-Fourth-Season/subtitles-70963.aspx"), 420);
	}
	

	@Test
	public void search() throws Exception {
		List<SearchResult> results = subscene.search("twin peaks");
		
		SubsceneSearchResult result = (SubsceneSearchResult) results.get(1);
		
		assertEquals(testResult.getName(), result.getName());
		assertEquals(testResult.getURL().toString(), result.getURL().toString());
		assertEquals(testResult.getSubtitleCount(), result.getSubtitleCount());
	}
	

	@Test
	public void searchResultPageRedirect() throws Exception {
		List<SearchResult> results = subscene.search("firefly");
		
		assertEquals(1, results.size());
		
		SubsceneSearchResult result = (SubsceneSearchResult) results.get(0);
		
		assertEquals("Firefly - The Complete Series", result.getName());
		assertEquals("http://subscene.com/Firefly-The-Complete-Series/subtitles-20008.aspx", result.getURL().toString());
		assertEquals(15, result.getSubtitleCount());
	}
	

	@Test
	public void getSubtitleListSearchResult() throws Exception {
		List<SubtitleDescriptor> subtitleList = subscene.getSubtitleList(testResult, Locale.ITALIAN);
		
		assertEquals(1, subtitleList.size());
		
		SubtitleDescriptor subtitle = subtitleList.get(0);
		
		assertEquals("Twin Peaks - First Season", subtitle.getName());
		assertEquals("Italian", subtitle.getLanguageName());
		assertEquals("zip", subtitle.getArchiveType());
	}
	

	@Test
	public void getSubtitleListSearchResultMany() throws Exception {
		List<SubtitleDescriptor> subtitleList = subscene.getSubtitleList(manySubtitlesTestResult, LanguageResolver.getDefault().getLocale("Vietnamese"));
		
		assertEquals(1, subtitleList.size());
	}
	

	@Test
	public void getSubtitleListLink() throws Exception {
		assertEquals(testResult.getURL().toString(), subscene.getSubtitleListLink(testResult).toURL().toString());
	}
	
}
