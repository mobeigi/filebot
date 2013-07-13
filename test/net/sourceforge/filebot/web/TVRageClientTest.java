
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;



public class TVRageClientTest {
	
	/**
	 * 145 episodes / 7 seasons
	 */
	private static TVRageSearchResult buffySearchResult = new TVRageSearchResult("Buffy the Vampire Slayer", 2930, "http://www.tvrage.com/Buffy_The_Vampire_Slayer");
	
	
	@Test
	public void search() throws Exception {
		List<SearchResult> results = tvrage.search("Buffy");
		
		TVRageSearchResult result = (TVRageSearchResult) results.get(0);
		
		assertEquals(buffySearchResult.getName(), result.getName());
		assertEquals(buffySearchResult.getSeriesId(), result.getSeriesId());
		assertEquals(buffySearchResult.getLink(), result.getLink());
	}
	
	
	private TVRageClient tvrage = new TVRageClient();
	
	
	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> list = EpisodeUtilities.filterBySeason(tvrage.getEpisodeList(buffySearchResult), 7);
		
		assertEquals(22, list.size());
		
		Episode chosen = list.get(21);
		
		assertEquals("Buffy the Vampire Slayer", chosen.getSeriesName());
		assertEquals("1997-03-10", chosen.getSeriesStartDate().toString());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getEpisode().toString());
		assertEquals("7", chosen.getSeason().toString());
		assertEquals(null, chosen.getAbsolute());
		assertEquals("2003-05-20", chosen.getAirdate().toString());
	}
	
	
	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = tvrage.getEpisodeList(buffySearchResult);
		
		assertEquals(144, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals(null, first.getAbsolute());
		assertEquals("1997-03-10", first.getAirdate().toString());
	}
	
	
	@Test
	public void getEpisodeListLinkAll() throws Exception {
		assertEquals(tvrage.getEpisodeListLink(buffySearchResult).toString(), "http://www.tvrage.com/Buffy_The_Vampire_Slayer/episode_list/all");
	}
	
}
