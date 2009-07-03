
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;


public class TVDotComClientTest {
	
	private static TVDotComClient tvdotcom = new TVDotComClient();
	
	private static HyperLink buffySearchResult;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		buffySearchResult = new HyperLink("Buffy the Vampire Slayer", new URL("http://www.tv.com/buffy-the-vampire-slayer/show/10/episode.html"));
	}
	

	@Test
	public void search() throws Exception {
		List<SearchResult> results = tvdotcom.search("Buffy");
		
		// if this fails, there is probably a problem with the xpath query
		assertEquals(10, results.size());
		
		HyperLink result = (HyperLink) results.get(0);
		
		assertEquals(buffySearchResult.getName(), result.getName());
		assertEquals(buffySearchResult.getURL().toString(), result.getURL().toString());
	}
	

	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(buffySearchResult, 7);
		
		assertEquals(22, list.size());
		
		Episode chosen = list.get(21);
		
		assertEquals("Buffy the Vampire Slayer", chosen.getSeriesName());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getEpisode());
		assertEquals("7", chosen.getSeason());
	}
	

	@Test
	public void getEpisodeListAllMultiSeason() throws Exception {
		// 144 episodes / 7 seasons
		List<Episode> list = tvdotcom.getEpisodeList(buffySearchResult);
		
		assertEquals(144, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode());
		assertEquals("1", first.getSeason());
	}
	

	@Test
	public void getEpisodeListAllSingleSeason() throws Exception {
		// 13 episodes / 1 season only
		List<Episode> list = tvdotcom.getEpisodeList(tvdotcom.search("Firefly").get(0));
		
		assertEquals(15, list.size());
		
		Episode fourth = list.get(3);
		
		assertEquals("Firefly", fourth.getSeriesName());
		assertEquals("Jaynestown", fourth.getTitle());
		assertEquals("4", fourth.getEpisode());
		assertEquals("1", fourth.getSeason());
	}
	

	@Test
	public void getEpisodeListAllManySeasons() throws Exception {
		// more than 700 episodes / 26 seasons
		List<Episode> list = tvdotcom.getEpisodeList(tvdotcom.search("Doctor Who (1963)").get(0));
		
		// there are still new episodes coming out
		assertTrue(list.size() > 700);
	}
	

	@Test
	public void getEpisodeListEncoding() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(tvdotcom.search("Lost").get(0), 3);
		
		Episode episode = list.get(16);
		
		assertEquals("Lost", episode.getSeriesName());
		assertEquals("Exposé", episode.getTitle());
		assertEquals("14", episode.getEpisode());
		assertEquals("3", episode.getSeason());
	}
	

	@Test
	public void getEpisodeListLink() {
		assertEquals(tvdotcom.getEpisodeListLink(buffySearchResult, 1).toString(), "http://www.tv.com/buffy-the-vampire-slayer/show/10/episode.html?season=1");
	}
	

	@Test
	public void getEpisodeListLinkAll() {
		assertEquals(tvdotcom.getEpisodeListLink(buffySearchResult, 0).toString(), "http://www.tv.com/buffy-the-vampire-slayer/show/10/episode.html?season=0");
	}
	
}
