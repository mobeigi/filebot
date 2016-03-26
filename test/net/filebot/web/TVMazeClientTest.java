package net.filebot.web;

import static net.filebot.web.EpisodeUtilities.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class TVMazeClientTest {

	/**
	 * 145 episodes / 7 seasons
	 */
	SearchResult buffySearchResult = new SearchResult(427, "Buffy the Vampire Slayer");

	@Test
	public void search() throws Exception {
		List<SearchResult> results = client.search("Buffy", Locale.ENGLISH);

		SearchResult result = results.get(0);

		assertEquals(buffySearchResult.getName(), result.getName());
		assertEquals(buffySearchResult.getId(), result.getId());
	}

	private TVMazeClient client = new TVMazeClient();

	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> list = filterBySeason(client.getEpisodeList(buffySearchResult, SortOrder.Airdate, Locale.ENGLISH), 7);

		assertEquals(22, list.size());

		Episode chosen = list.get(21);

		assertEquals("Buffy the Vampire Slayer", chosen.getSeriesName());
		assertEquals("1997-03-10", chosen.getSeriesInfo().getStartDate().toString());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getEpisode().toString());
		assertEquals("7", chosen.getSeason().toString());
		assertEquals(null, chosen.getAbsolute());
		assertEquals("2003-05-20", chosen.getAirdate().toString());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = client.getEpisodeList(buffySearchResult, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(143, list.size());

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
		assertEquals("http://www.tvmaze.com/shows/427", client.getEpisodeListLink(buffySearchResult).toString());
	}

}
