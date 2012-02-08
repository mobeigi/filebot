
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;


public class IMDbClientTest {
	
	private final IMDbClient imdb = new IMDbClient();
	
	
	@Test
	public void searchMovie() throws Exception {
		List<Movie> results = imdb.searchMovie("Avatar", null);
		
		assertEquals(26, results.size());
		Movie movie = (Movie) results.get(0);
		
		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void searchMovieRedirect() throws Exception {
		List<Movie> results = imdb.searchMovie("battle angel alita", null);
		
		assertEquals(1, results.size());
		Movie movie = (Movie) results.get(0);
		
		assertEquals("Battle Angel", movie.getName());
		assertEquals(1993, movie.getYear());
		assertEquals(107061, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void getMovieDescriptor() throws Exception {
		Movie movie = imdb.getMovieDescriptor(499549, null);
		
		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}
	
}
