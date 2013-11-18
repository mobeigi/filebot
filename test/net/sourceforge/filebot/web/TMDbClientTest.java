package net.sourceforge.filebot.web;

import static net.sourceforge.filebot.Settings.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.sourceforge.filebot.web.TMDbClient.Artwork;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo;

import org.junit.Test;

public class TMDbClientTest {

	private final TMDbClient tmdb = new TMDbClient(getApplicationProperty("themoviedb.apikey"));

	@Test
	public void searchByName() throws Exception {
		List<Movie> result = tmdb.searchMovie("Serenity", Locale.CHINESE);
		Movie movie = result.get(0);

		assertEquals("冲出宁静号", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(16320, movie.getTmdbId());
	}

	@Test
	public void searchByNameGermanResults() throws Exception {
		List<Movie> result = tmdb.searchMovie("East of Eden", Locale.GERMAN);
		Movie movie = result.get(0);

		assertEquals("Jenseits von Eden", movie.getName());
		assertEquals(1955, movie.getYear());
		assertEquals(Arrays.asList("Jenseits von Eden (1955)", "East of Eden (1955)"), movie.getEffectiveNames());
	}

	@Test
	public void searchByIMDB() throws Exception {
		Movie movie = tmdb.getMovieDescriptor(418279, Locale.ENGLISH);

		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear(), 0);
		assertEquals(418279, movie.getImdbId(), 0);
		assertEquals(1858, movie.getTmdbId(), 0);
	}

	@Test
	public void getMovieInfo() throws Exception {
		MovieInfo movie = tmdb.getMovieInfo(new Movie(null, 0, 418279, -1), Locale.ENGLISH);

		assertEquals("Transformers", movie.getName());
		assertEquals("2007-07-02", movie.getReleased().toString());
		assertEquals("PG-13", movie.getCertification());
		assertEquals("[en]", movie.getSpokenLanguages().toString());
		assertEquals("Shia LaBeouf", movie.getActors().get(0));
		assertEquals("Michael Bay", movie.getDirector());
		assertEquals("Editor", movie.getCast().get(30).getJob());
	}

	@Test
	public void getArtwork() throws Exception {
		List<Artwork> artwork = tmdb.getArtwork("tt0418279");
		assertEquals("backdrops", artwork.get(0).getCategory());
		assertEquals("http://d3gtl9l2a4fn1j.cloudfront.net/t/p/original/jC4bQLEEcpM8N7BjpkMtP0zPakJ.jpg", artwork.get(0).getUrl().toString());
	}

	@Test
	public void floodLimit() throws Exception {
		for (Locale it : Locale.getAvailableLocales()) {
			List<Movie> results = tmdb.searchMovie("Serenity", it);
			assertEquals(16320, results.get(0).getTmdbId());
		}
	}

}
