
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;


public class IMDbClientTest {
	
	private final IMDbClient imdb = new IMDbClient();
	
	
	@Test
	public void searchMovie() throws Exception {
		List<Movie> results = imdb.searchMovie("Avatar", null);
		Movie movie = results.get(0);
		
		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void searchMovie2() throws Exception {
		List<Movie> results = imdb.searchMovie("Heat", null);
		Movie movie = results.get(0);
		
		assertEquals("Heat", movie.getName());
		assertEquals(1995, movie.getYear());
		assertEquals(113277, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void searchMovieRedirect() throws Exception {
		List<Movie> results = imdb.searchMovie("(500) Days of Summer (2009)", null);
		
		Movie movie = results.get(0);
		
		assertEquals("(500) Days of Summer", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(1022603, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void getMovieDescriptor() throws Exception {
		Movie movie = imdb.getMovieDescriptor(499549, null);
		
		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void getAkaMovieDescriptor() throws Exception {
		Movie movie = imdb.getMovieDescriptor(106559, Locale.ENGLISH);
		
		assertEquals("Ching Se", movie.getName());
		assertEquals(1993, movie.getYear());
		assertEquals(106559, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void getAkaMovieDescriptorExtra() throws Exception {
		Movie movie = imdb.getMovieDescriptor(470761, Locale.ENGLISH);
		
		assertEquals("First Born", movie.getName());
		assertEquals(2007, movie.getYear());
		assertEquals(470761, movie.getImdbId(), 0);
	}
	
}
