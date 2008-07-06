
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.Test;


public class TVDotComClientTest {
	
	private static TVDotComClient tvdotcom = new TVDotComClient();
	
	private static HyperLink testResult = new HyperLink("Buffy the Vampire Slayer", URI.create("http://www.tv.com/buffy-the-vampire-slayer/show/10/episode_listings.html"));
	private static HyperLink singleSeasonTestResult = new HyperLink("Firefly", URI.create("http://www.tv.com/firefly/show/7097/episode_listings.html"));
	private static HyperLink manySeasonsTestResult = new HyperLink("Doctor Who", URI.create("http://www.tv.com/doctor-who/show/355/episode_listings.html"));
	
	
	@Test
	public void search() throws Exception {
		List<SearchResult> results = tvdotcom.search("Buffy");
		
		HyperLink result = (HyperLink) results.get(0);
		
		assertEquals(testResult.getName(), result.getName());
		assertEquals(testResult.getURI(), result.getURI());
	}
	

	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> results = tvdotcom.getEpisodeList(testResult, 7);
		
		assertEquals(22, results.size());
		
		Episode chosen = results.get(21);
		
		assertEquals("Buffy the Vampire Slayer", chosen.getShowName());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getNumberOfEpisode());
		assertEquals("7", chosen.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllMultiSeason() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(testResult);
		
		assertEquals(145, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getShowName());
		assertEquals("Unaired Pilot", first.getTitle());
		assertEquals("Pilot", first.getNumberOfEpisode());
		assertEquals("1", first.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllSingleSeason() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(singleSeasonTestResult);
		
		assertEquals(15, list.size());
		
		Episode fourth = list.get(3);
		
		assertEquals("Firefly", fourth.getShowName());
		assertEquals("Jaynestown", fourth.getTitle());
		assertEquals("04", fourth.getNumberOfEpisode());
		assertEquals("1", fourth.getNumberOfSeason());
	}
	

	@Test
	public void getEpisodeListAllManySeasons() throws Exception {
		List<Episode> list = tvdotcom.getEpisodeList(manySeasonsTestResult);
		
		assertEquals(708, list.size());
	}
	

	@Test
	public void getEpisodeListLink() {
		assertEquals(tvdotcom.getEpisodeListLink(testResult, 1).toString(), "http://www.tv.com/buffy-the-vampire-slayer/show/10/episode_listings.html?season=1");
	}
	

	@Test
	public void getEpisodeListLinkAll() {
		assertEquals(tvdotcom.getEpisodeListLink(testResult, 0).toString(), "http://www.tv.com/buffy-the-vampire-slayer/show/10/episode_listings.html?season=0");
	}
	
}
