package net.filebot.web;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import net.filebot.web.TheTVDBClientV1.MirrorType;

public class TheTVDBClientV1Test {

	TheTVDBClientV1 db = new TheTVDBClientV1("BA864DEE427E384A");

	SearchResult buffy = new SearchResult(70327, "Buffy the Vampire Slayer");
	SearchResult wonderfalls = new SearchResult(78845, "Wonderfalls");
	SearchResult firefly = new SearchResult(78874, "Firefly");
	SearchResult dracula = new SearchResult(313193, "Dracula (2016)"); // DOES NOT EXIST

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = db.search("babylon 5", Locale.ENGLISH);

		assertEquals(2, results.size());

		SearchResult first = results.get(0);

		assertEquals("Babylon 5", first.getName());
		assertEquals(70726, first.getId());
	}

	@Test
	public void searchGerman() throws Exception {
		List<SearchResult> results = db.search("Buffy the Vampire Slayer", Locale.GERMAN);

		assertEquals(2, results.size());

		SearchResult first = results.get(0);

		assertEquals("Buffy the Vampire Slayer", first.getName());
		assertEquals(70327, first.getId());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = db.getEpisodeList(buffy, SortOrder.Airdate, Locale.ENGLISH);

		assertTrue(list.size() >= 144);

		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-03-10", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("1997-03-10", first.getAirdate().toString());
	}

	@Test
	public void getEpisodeListNull() throws Exception {
		List<Episode> list = db.getEpisodeList(dracula, SortOrder.Airdate, Locale.ENGLISH);
		assertTrue(list.isEmpty());
	}

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = db.getEpisodeList(wonderfalls, SortOrder.Airdate, Locale.ENGLISH);

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
		List<Episode> list = db.getEpisodeList(firefly, SortOrder.DVD, Locale.ENGLISH);

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
		assertEquals("http://www.thetvdb.com/?tab=seasonall&id=78874", db.getEpisodeListLink(firefly).toString());
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
		SearchResult series = db.lookupByID(78874, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getId());
	}

	@Test
	public void lookupByIMDbID() throws Exception {
		SearchResult series = db.lookupByIMDbID(303461, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getId());
	}

	@Test
	public void getSeriesInfo() throws Exception {
		TheTVDBSeriesInfo it = (TheTVDBSeriesInfo) db.getSeriesInfo(80348, Locale.ENGLISH);

		assertEquals(80348, it.getId(), 0);
		assertEquals("TV-PG", it.getCertification());
		assertEquals("2007-09-24", it.getStartDate().toString());
		assertEquals("Action", it.getGenres().get(0));
		assertEquals("tt0934814", it.getImdbId());
		assertEquals("en", it.getLanguage());
		assertEquals("45", it.getRuntime().toString());
		assertEquals("Chuck", it.getName());
	}

	@Test
	public void getBanner() throws Exception {
		Artwork banner = db.getArtwork(buffy.getId(), "season", Locale.ROOT).stream().filter(it -> {
			return it.matches("season", "seasonwide", "7", "en");
		}).findFirst().get();

		assertEquals("season", banner.getTags().get(0));
		assertEquals("seasonwide", banner.getTags().get(1));
		assertEquals("http://thetvdb.com/banners/seasonswide/70327-7.jpg", banner.getUrl().toString());
		assertEquals(99712, WebRequest.fetch(banner.getUrl()).remaining(), 0);
	}

}
