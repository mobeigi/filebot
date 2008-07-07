
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;


public class AnidbClientTest {
	
	private static HyperLink testResult;
	private static HyperLink shortLinkTestResult;
	
	private AnidbClient anidb = new AnidbClient();
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testResult = new HyperLink("Monster", new URL("http://anidb.net/perl-bin/animedb.pl?show=anime&aid=1539"));
		shortLinkTestResult = new HyperLink("Juuni Kokuki", new URL("http://anidb.net/a26"));
	}
	

	@Test
	public void search() throws Exception {
		List<SearchResult> results = anidb.search("one piece");
		
		HyperLink result = (HyperLink) results.get(0);
		
		assertEquals("One Piece", result.getName());
		assertEquals("http://anidb.net/perl-bin/animedb.pl?show=anime&aid=69", result.getURL().toString());
	}
	

	@Test
	public void searchResultPageRedirect() throws Exception {
		List<SearchResult> results = anidb.search("twelve kingdoms");
		
		assertEquals(1, results.size());
		
		HyperLink result = (HyperLink) results.get(0);
		
		assertEquals("Juuni Kokuki", result.getName());
		assertEquals("http://anidb.net/a26", result.getURL().toString());
	}
	

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = anidb.getEpisodeList(testResult);
		
		assertEquals(74, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Monster", first.getShowName());
		assertEquals("Herr Dr. Tenma", first.getTitle());
		assertEquals("01", first.getNumberOfEpisode());
		assertEquals(null, first.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllShortLink() throws Exception {
		List<Episode> list = anidb.getEpisodeList(shortLinkTestResult);
		
		assertEquals(45, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Juuni Kokuki", first.getShowName());
		assertEquals("Shadow of the Moon, The Sea of Shadow - Chapter 1", first.getTitle());
		assertEquals("01", first.getNumberOfEpisode());
		assertEquals(null, first.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListLink() throws Exception {
		assertEquals(testResult.getURL().toString(), anidb.getEpisodeListLink(testResult).toURL().toString());
	}
	
}
