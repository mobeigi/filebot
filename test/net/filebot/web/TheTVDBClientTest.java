package net.filebot.web;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import net.filebot.web.TheTVDBClient.MirrorType;

public class TheTVDBClientTest {

	TheTVDBClient thetvdb = new TheTVDBClient("BA864DEE427E384A");

	SearchResult buffy = new SearchResult(70327, "Buffy the Vampire Slayer");
	SearchResult wonderfalls = new SearchResult(78845, "Wonderfalls");
	SearchResult firefly = new SearchResult(78874, "Firefly");

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
		assertEquals("Unaired Pilot", last.getTitle());
		assertEquals("1", last.getSeason().toString());
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
	public void resolveTypeMask() {
		// no flags set
		assertEquals(MirrorType.newSet(), MirrorType.fromTypeMask(0));

		// all flags set
		assertEquals(EnumSet.of(MirrorType.SEARCH, MirrorType.XML, MirrorType.BANNER), MirrorType.fromTypeMask(7));
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
		TheTVDBSeriesInfo it = (TheTVDBSeriesInfo) thetvdb.getSeriesInfo(80348, Locale.ENGLISH);

		assertEquals(80348, it.getId(), 0);
		assertEquals("TV-PG", it.getContentRating());
		assertEquals("2007-09-24", it.getFirstAired().toString());
		assertEquals("Action", it.getGenres().get(0));
		assertEquals("tt0934814", it.getImdbId());
		assertEquals("en", it.getLanguage());
		assertEquals(987, it.getOverview().length());
		assertEquals("45", it.getRuntime().toString());
		assertEquals("Chuck", it.getName());
	}

	@Test
	public void getBanner() throws Exception {
		Artwork banner = thetvdb.getArtwork(buffy.getId(), "season", Locale.ROOT).stream().filter(it -> {
			return it.getTags().contains("season") && it.getTags().contains("seasonwide") && it.getTags().contains("7") && it.getLanguage().equals("en");
		}).findFirst().get();

		assertEquals("season", banner.getTags().get(0));
		assertEquals("seasonwide", banner.getTags().get(1));
		assertEquals("http://thetvdb.com/banners/seasonswide/70327-7.jpg", banner.getUrl().toString());
		assertEquals(99712, WebRequest.fetch(banner.getUrl()).remaining(), 0);
	}

}
