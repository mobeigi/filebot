
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import net.sourceforge.filebot.web.TVRageClient.TVRageSearchResult;


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
		assertEquals(buffySearchResult.getShowId(), result.getShowId());
		assertEquals(buffySearchResult.getLink(), result.getLink());
	}
	

	private TVRageClient tvrage = new TVRageClient();
	

	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> list = tvrage.getEpisodeList(buffySearchResult, 7);
		
		assertEquals(22, list.size());
		
		Episode chosen = list.get(21);
		
		assertEquals("Buffy the Vampire Slayer", chosen.getSeriesName());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getEpisode());
		assertEquals("7", chosen.getSeason());
		assertEquals("2003-05-20", chosen.airdate().toString());
	}
	

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = tvrage.getEpisodeList(buffySearchResult);
		
		assertEquals(145, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("Unaired Pilot", first.getTitle());
		assertEquals("00", first.getEpisode());
		assertEquals("0", first.getSeason());
		assertEquals(null, first.airdate());
	}
	

	@Test(expected = SeasonOutOfBoundsException.class)
	public void getEpisodeListIllegalSeason() throws Exception {
		tvrage.getEpisodeList(buffySearchResult, 42);
	}
	

	@Test
	public void getEpisodeListLink() throws Exception {
		assertEquals(tvrage.getEpisodeListLink(buffySearchResult, 1).toString(), "http://www.tvrage.com/Buffy_The_Vampire_Slayer/episode_list/1");
	}
	

	@Test
	public void getEpisodeListLinkAll() throws Exception {
		assertEquals(tvrage.getEpisodeListLink(buffySearchResult).toString(), "http://www.tvrage.com/Buffy_The_Vampire_Slayer/episode_list/all");
	}
	
}
