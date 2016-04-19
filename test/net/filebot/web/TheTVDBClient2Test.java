package net.filebot.web;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class TheTVDBClient2Test {

	static TheTVDBClient2 thetvdb = new TheTVDBClient2("BA864DEE427E384A");

	SearchResult buffy = new SearchResult(70327, "Buffy the Vampire Slayer");
	SearchResult wonderfalls = new SearchResult(78845, "Wonderfalls");
	SearchResult firefly = new SearchResult(78874, "Firefly");

	@Test
	public void languages() throws Exception {
		String[] languages = thetvdb.languages();
		assertEquals("[zh, en, sv, no, da, fi, nl, de, it, es, fr, pl, hu, el, tr, ru, he, ja, pt, cs, sl, hr, ko]", asList(languages).toString());
	}

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = thetvdb.search("babylon 5", Locale.ENGLISH);

		assertEquals(2, results.size());

		SearchResult first = results.get(0);

		assertEquals("Babylon 5", first.getName());
		assertEquals(70726, first.getId());
	}

	@Test
	public void searchGerman() throws Exception {
		List<SearchResult> results = thetvdb.search("Buffy the Vampire Slayer", Locale.GERMAN);

		assertEquals(2, results.size());

		SearchResult first = results.get(0);

		assertEquals("Buffy the Vampire Slayer", first.getName());
		assertEquals(70327, first.getId());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(buffy, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(145, list.size());

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
		assertEquals("Unaired Pilot", last.getTitle());
		assertEquals(null, last.getSeason());
		assertEquals(null, last.getEpisode());
		assertEquals(null, last.getAbsolute());
		assertEquals("1", last.getSpecial().toString());
		assertEquals(null, last.getAirdate());
	}

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(wonderfalls, SortOrder.Airdate, Locale.ENGLISH);

		Episode first = list.get(0);

		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("2004-03-12", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Wax Lion", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals(null, first.getAbsolute()); // should be "1" but data has not yet been entered
		assertEquals("2004-03-12", first.getAirdate().toString());
	}

	@Test
	public void getEpisodeListNumbering() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(firefly, SortOrder.DVD, Locale.ENGLISH);

		Episode first = list.get(0);
		assertEquals("Firefly", first.getSeriesName());
		assertEquals("2002-09-20", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Serenity", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("2002-12-20", first.getAirdate().toString());
	}

	public void getEpisodeListLink() {
		assertEquals("http://www.thetvdb.com/?tab=seasonall&id=78874", thetvdb.getEpisodeListLink(firefly).toString());
	}

	@Test
	public void lookupByID() throws Exception {
		SearchResult series = thetvdb.lookupByID(78874, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getId());
	}

	@Test
	public void lookupByIMDbID() throws Exception {
		SearchResult series = thetvdb.lookupByIMDbID(303461, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getId());
	}

	@Test
	public void getSeriesInfo() throws Exception {
		SeriesInfo it = thetvdb.getSeriesInfo(80348, Locale.ENGLISH);

		assertEquals(80348, it.getId(), 0);
		assertEquals("Action", it.getGenres().get(0));
		assertEquals("en", it.getLanguage());
		assertEquals("45", it.getRuntime().toString());
		assertEquals("Chuck", it.getName());
	}

	@Test
	public void getImages() throws Exception {
		Artwork i = thetvdb.getArtwork(buffy.getId(), "fanart", Locale.ENGLISH).get(0);

		assertEquals("[fanart, 1280x720]", i.getTags().toString());
		assertEquals("http://thetvdb.com/banners/fanart/original/70327-1.jpg", i.getUrl().toString());
		assertTrue(i.matches("fanart", "1280x720"));
		assertFalse(i.matches("fanart", "1280x720", "1"));
	}

}
