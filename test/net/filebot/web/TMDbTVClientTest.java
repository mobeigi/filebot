package net.filebot.web;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class TMDbTVClientTest {

	TMDbTVClient tmdb = new TMDbTVClient(new TMDbClient("66308fb6e3fd850dde4c7d21df2e8306"));

	SearchResult buffy = new SearchResult(95, "Buffy the Vampire Slayer");
	SearchResult wonderfalls = new SearchResult(1982, "Wonderfalls");
	SearchResult firefly = new SearchResult(1437, "Firefly");

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = tmdb.search("babylon 5", Locale.ENGLISH);

		assertEquals(1, results.size());

		assertEquals("Babylon 5", results.get(0).getName());
		assertEquals(3137, results.get(0).getId());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = tmdb.getEpisodeList(buffy, SortOrder.Airdate, Locale.ENGLISH);

		assertTrue(list.size() >= 144);

		// check ordinary episode
		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-03-10", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("1997-03-10", first.getAirdate().toString());

		// check special episode
		Episode last = list.get(list.size() - 1);
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Unaired Buffy the Vampire Slayer pilot", last.getTitle());
		assertEquals(null, last.getSeason());
		assertEquals(null, last.getEpisode());
		assertEquals(null, last.getAbsolute());
		assertEquals("1", last.getSpecial().toString());
		assertEquals(null, last.getAirdate());
	}

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = tmdb.getEpisodeList(wonderfalls, SortOrder.Airdate, Locale.ENGLISH);

		Episode first = list.get(0);
		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("2004-03-12", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Wax Lion", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("2004-03-12", first.getAirdate().toString());
	}

}
