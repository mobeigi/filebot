package net.filebot.web;

import static net.filebot.CachedResource.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.CachedResource;
import net.filebot.web.TMDbClient.MovieInfo;

public class TMDbClientTest {

	static TMDbClient db = new TMDbClient("66308fb6e3fd850dde4c7d21df2e8306");

	@Test
	public void searchByName() throws Exception {
		List<Movie> result = db.searchMovie("Serenity", Locale.CHINESE);
		Movie movie = result.get(0);

		assertEquals("冲出宁静号", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(16320, movie.getTmdbId());
	}

	@Test
	public void searchByNameWithYearShortName() throws Exception {
		List<Movie> result = db.searchMovie("Up 2009", Locale.ENGLISH);
		Movie movie = result.get(0);

		assertEquals("Up", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(14160, movie.getTmdbId());
	}

	@Test
	public void searchByNameWithYearNumberName() throws Exception {
		List<Movie> result = db.searchMovie("9 (2009)", Locale.ENGLISH);
		Movie movie = result.get(0);

		assertEquals("9", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(12244, movie.getTmdbId());
	}

	@Test
	public void searchByNameGermanResults() throws Exception {
		List<Movie> result = db.searchMovie("East of Eden", Locale.GERMAN);
		Movie movie = result.get(0);

		assertEquals("Jenseits von Eden", movie.getName());
		assertEquals(1955, movie.getYear());
		assertEquals(Arrays.asList("Jenseits von Eden (1955)", "East of Eden (1955)"), movie.getEffectiveNames());
	}

	@Test
	public void searchByIMDB() throws Exception {
		Movie movie = db.getMovieDescriptor(new Movie(418279), Locale.ENGLISH);

		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear(), 0);
		assertEquals(418279, movie.getImdbId(), 0);
		assertEquals(1858, movie.getTmdbId(), 0);
	}

	@Test
	public void getMovieInfo() throws Exception {
		MovieInfo movie = db.getMovieInfo(new Movie(418279), Locale.ENGLISH, true);

		assertEquals("Transformers", movie.getName());
		assertEquals("2007-06-27", movie.getReleased().toString());
		assertEquals("PG-13", movie.getCertification());
		assertEquals("[es, en]", movie.getSpokenLanguages().toString());
		assertEquals("Shia LaBeouf", movie.getActors().get(0));
		assertEquals("Michael Bay", movie.getDirector());
	}

	@Test
	public void getAlternativeTitles() throws Exception {
		Map<String, List<String>> titles = db.getAlternativeTitles(16320); // Serenity

		assertEquals("[衝出寧靜號]", titles.get("TW").toString());
		assertEquals("[萤火虫, 宁静号]", titles.get("CN").toString());
	}

	@Test
	public void getArtwork() throws Exception {
		Artwork a = db.getArtwork(16320, "backdrops", Locale.ROOT).get(0);
		assertEquals("[backdrops, 1920x1080]", a.getTags().toString());
		assertEquals("https://image.tmdb.org/t/p/original/424MxHQe5Hfu92hTeRvZb5Giv0X.jpg", a.getUrl().toString());
	}

	@Test
	public void getPeople() throws Exception {
		Person p = db.getMovieInfo("16320", Locale.ENGLISH, true).getPeople().get(0);
		assertEquals("Nathan Fillion", p.getName());
		assertEquals("Mal", p.getCharacter());
		assertEquals(null, p.getJob());
		assertEquals(null, p.getDepartment());
		assertEquals("0", p.getOrder().toString());
		assertEquals("http://image.tmdb.org/t/p/original/B7VTVtnKyFk0AtYjEbqzBQlPec.jpg", p.getImage().toString());
	}

	@Test
	public void discoverPeriod() throws Exception {
		Movie m = db.discover(LocalDate.parse("2014-09-15"), LocalDate.parse("2014-10-22"), Locale.ENGLISH).get(0);

		assertEquals("Big Hero 6", m.getName());
		assertEquals(2014, m.getYear());
		assertEquals(177572, m.getTmdbId());
	}

	@Test
	public void discoverBestOfYear() throws Exception {
		Movie m = db.discover(2015, Locale.ENGLISH).get(0);

		assertEquals("Mad Max: Fury Road", m.getName());
		assertEquals(2015, m.getYear());
		assertEquals(76341, m.getTmdbId());
	}

	@Ignore
	@Test
	public void floodLimit() throws Exception {
		for (Locale it : Locale.getAvailableLocales()) {
			List<Movie> results = db.searchMovie("Serenity", it);
			assertEquals(16320, results.get(0).getTmdbId());
		}
	}

	@Ignore
	@Test
	public void etag() throws Exception {
		Cache cache = Cache.getCache("test", CacheType.Persistent);
		Cache etagStorage = Cache.getCache("etag", CacheType.Persistent);
		CachedResource<String, byte[]> resource = cache.bytes("http://devel.squid-cache.org/old_projects.html#etag", URL::new).fetch(fetchIfNoneMatch(etagStorage::get, etagStorage::put)).expire(Duration.ZERO);
		assertArrayEquals(resource.get(), resource.get());
	}

}
