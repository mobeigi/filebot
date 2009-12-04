
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.Settings.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;


public class TMDbClientTest {
	
	private final TMDbClient tmdb = new TMDbClient(getApplicationProperty("themoviedb.apikey"));
	

	@Test
	public void searchByName() throws Exception {
		List<MovieDescriptor> result = tmdb.searchMovie("transformers");
		MovieDescriptor movie = result.get(0);
		
		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear());
		assertEquals(418279, movie.getImdbId());
	}
	

	@Test
	public void searchByHash() throws Exception {
		List<MovieDescriptor> results = tmdb.getMovies("Hash.getInfo", "2bba5c34b007153b");
		MovieDescriptor movie = results.get(0);
		
		assertEquals("Season 1", movie.getName());
		assertEquals(2002, movie.getYear());
		assertEquals(303461, movie.getImdbId());
	}
	

	@Test
	public void searchByIMDB() throws Exception {
		MovieDescriptor movie = tmdb.getMovieDescriptor(418279);
		
		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear());
		assertEquals(418279, movie.getImdbId());
	}
	
}
