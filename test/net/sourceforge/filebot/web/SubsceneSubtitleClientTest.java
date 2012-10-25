
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import net.sourceforge.filebot.web.SubsceneSubtitleClient.SubsceneSearchResult;

import org.junit.BeforeClass;
import org.junit.Test;


public class SubsceneSubtitleClientTest {
	
	/**
	 * Twin Peaks - First Season, ~ 15 subtitles
	 */
	private static HyperLink twinpeaksSearchResult;
	
	/**
	 * Lost - Fourth Season, ~ 430 subtitles
	 */
	private static HyperLink lostSearchResult;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		twinpeaksSearchResult = new SubsceneSearchResult("Twin Peaks", "Twin Peaks - First Season (1990)", new URL("http://subscene.com/subtitles/twin-peaks-first-season"));
		lostSearchResult = new SubsceneSearchResult("Lost", "Lost - Fourth Season (2008)", new URL("http://subscene.com/subtitles/lost-fourth-season"));
	}
	
	private SubsceneSubtitleClient subscene = new SubsceneSubtitleClient();
	
	
	@Test
	public void search() throws Exception {
		List<SearchResult> results = subscene.search("twin peaks");
		
		SubsceneSearchResult result = (SubsceneSearchResult) results.get(0);
		assertEquals(twinpeaksSearchResult.toString(), result.toString());
		assertEquals(twinpeaksSearchResult.getURL().toString(), result.getURL().toString());
		assertEquals(twinpeaksSearchResult.getName(), result.getName());
	}
	
	
	@Test
	public void search2() throws Exception {
		List<SearchResult> results = subscene.search("Avatar 2009");
		
		SubsceneSearchResult result = (SubsceneSearchResult) results.get(0);
		assertEquals("Firefly - The Complete Series (2002)", result.toString());
		assertEquals("Firefly", result.getName());
		assertEquals("http://subscene.com/subtitles/firefly-the-complete-series", result.getURL().toString());
	}
	
	
	@Test
	public void getSubtitleListSearchResult() throws Exception {
		List<SubtitleDescriptor> subtitleList = subscene.getSubtitleList(twinpeaksSearchResult, "Italian");
		assertEquals(10, subtitleList.size());
		
		SubtitleDescriptor subtitle = subtitleList.get(0);
		assertEquals("Twin-Peaks-S01E00-Pilot-eAlternate-ita sub by IScrew [www.ITALIANSHARE.net]", subtitle.getName());
		assertEquals("Italian", subtitle.getLanguageName());
	}
	
	
	@Test
	public void getSubtitleListSearchResultMany() throws Exception {
		List<SubtitleDescriptor> subtitleList = subscene.getSubtitleList(lostSearchResult, "Japanese");
		
		// lots of subtitles, but only a few Japanese ones
		assertEquals(16, subtitleList.size());
	}
	
	
	@Test
	public void getLanguageFilterMap() throws Exception {
		Map<String, String> filters = subscene.getLanguageFilterMap();
		
		assertEquals("1", filters.get("albanian"));
		assertEquals("13", filters.get("english"));
		assertEquals("17", filters.get("finnish"));
		assertEquals("45", filters.get("vietnamese"));
	}
	
	
	@Test
	public void getSubtitleListLink() throws Exception {
		assertEquals(twinpeaksSearchResult.getURL().toString(), subscene.getSubtitleListLink(twinpeaksSearchResult, null).toURL().toString());
	}
	
	
	@Test
	public void downloadSubtitleArchive() throws Exception {
		SearchResult selectedResult = subscene.search("firefly").get(0);
		SubtitleDescriptor subtitleDescriptor = subscene.getSubtitleList(selectedResult, "English").get(0);
		assertEquals("Firefly.S01E00-13.DVDRip-Rogue.eng-RETAIL", subtitleDescriptor.getName());
		
		ByteBuffer archive = subtitleDescriptor.fetch();
		assertEquals(254549, archive.remaining());
	}
	
}
