
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.Settings.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;


public class TMDbClientTest {
	
	private final TMDbClient tmdb = new TMDbClient(getApplicationProperty("themoviedb.apikey"));
	

	@Test
	public void searchByName() throws Exception {
		List<Movie> result = tmdb.searchMovie("Serenity", Locale.CHINESE);
		Movie movie = result.get(0);
		
		assertEquals("冲出宁静号", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(379786, movie.getImdbId());
	}
	

	@Test
	public void searchByHash() throws Exception {
		List<Movie> results = tmdb.searchMovie("907172e7fe51ba57", 742086656, Locale.ENGLISH);
		Movie movie = results.get(0);
		
		assertEquals("Sin City", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(401792, movie.getImdbId());
	}
	

	@Test
	public void searchByIMDB() throws Exception {
		Movie movie = tmdb.getMovieDescriptor(418279, Locale.ENGLISH);
		
		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear());
		assertEquals(418279, movie.getImdbId());
	}
	
}
