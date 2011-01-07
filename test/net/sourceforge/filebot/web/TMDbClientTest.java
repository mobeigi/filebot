
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.Settings.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;


public class TMDbClientTest {
	
	private final TMDbClient tmdb = new TMDbClient(getApplicationProperty("themoviedb.apikey"));
	

	@Test
	public void searchByName() throws Exception {
		List<MovieDescriptor> result = tmdb.searchMovie("Serenity");
		MovieDescriptor movie = result.get(0);
		
		assertEquals("Serenity", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(379786, movie.getImdbId());
	}
	

	@Test
	public void searchByHash() throws Exception {
		List<MovieDescriptor> results = tmdb.searchMovie("907172e7fe51ba57", 742086656);
		MovieDescriptor movie = results.get(0);
		
		assertEquals("Sin City", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(401792, movie.getImdbId());
	}
	

	@Test
	public void searchByIMDB() throws Exception {
		MovieDescriptor movie = tmdb.getMovieDescriptor(418279);
		
		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear());
		assertEquals(418279, movie.getImdbId());
	}
	
}
