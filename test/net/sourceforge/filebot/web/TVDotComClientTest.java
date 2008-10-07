
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;


public class TVDotComClientTest {
	
	/**
	 * 145 episodes / 7 seasons
	 */
	private static HyperLink buffySearchResult;
	
	/**
	 * 13 episodes / 1 season only
	 */
	private static HyperLink fireflySearchResult;
	
	/**
	 * more than 700 episodes / 26 seasons (on going)
	 */
	private static HyperLink doctorwhoTestResult;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		buffySearchResult = new HyperLink("Buffy the Vampire Slayer", new URL("http://www.tv.com/buffy-the-vampire-slayer/show/10/episode_listings.html"));
		fireflySearchResult = new HyperLink("Firefly", new URL("http://www.tv.com/firefly/show/7097/episode_listings.html"));
		doctorwhoTestResult = new HyperLink("Doctor Who", new URL("http://www.tv.com/doctor-who/show/355/episode_listings.html"));
	}
	
	private TVDotComClient tvdotcom = new TVDotComClient();
	
	
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
		List<Episode> results = tvdotcom.getEpisodeList(buffySearchResult, 7);
		
		assertEquals(22, results.size());
		
		Episode chosen = results.get(21);
		
		assertEquals("Buffy the Vampire Slayer", chosen.getShowName());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getNumberOfEpisode());
		assertEquals("7", chosen.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllMultiSeason() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(buffySearchResult);
		
		assertEquals(145, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getShowName());
		assertEquals("Unaired Pilot", first.getTitle());
		assertEquals("Pilot", first.getNumberOfEpisode());
		assertEquals(null, first.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllSingleSeason() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(fireflySearchResult);
		
		assertEquals(15, list.size());
		
		Episode fourth = list.get(3);
		
		assertEquals("Firefly", fourth.getShowName());
		assertEquals("Jaynestown", fourth.getTitle());
		assertEquals("04", fourth.getNumberOfEpisode());
		assertEquals("1", fourth.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllManySeasons() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(doctorwhoTestResult);
		
		// there are still new episodes coming out
		assertTrue(list.size() > 700);
	}
	

	@Test
	public void getEpisodeListEncoding() throws Exception {
		HyperLink lostTestResult = new HyperLink("Lost", new URL("http://www.tv.com/lost/show/24313/episode_listings.html"));
		
		List<Episode> list = tvdotcom.getEpisodeList(lostTestResult, 3);
		
		Episode episode = list.get(13);
		
		assertEquals("Lost", episode.getShowName());
		assertEquals("Exposé", episode.getTitle());
		assertEquals("14", episode.getNumberOfEpisode());
		assertEquals("3", episode.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListLink() {
		assertEquals(tvdotcom.getEpisodeListLink(buffySearchResult, 1).toString(), "http://www.tv.com/buffy-the-vampire-slayer/show/10/episode_listings.html?season=1");
	}
	

	@Test
	public void getEpisodeListLinkAll() {
		assertEquals(tvdotcom.getEpisodeListLink(buffySearchResult, 0).toString(), "http://www.tv.com/buffy-the-vampire-slayer/show/10/episode_listings.html?season=0");
	}
	
}
