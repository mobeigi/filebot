
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;


public class IMDbClientTest {
	
	private final IMDbClient imdb = new IMDbClient();
	
	
	@Test
	public void searchMovie1() throws Exception {
		List<Movie> results = imdb.searchMovie("Avatar", null);
		Movie movie = results.get(0);
		
		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void searchMovie2() throws Exception {
		List<Movie> results = imdb.searchMovie("The Illusionist", null);
		Movie movie = results.get(0);
		
		assertEquals("The Illusionist", movie.getName());
		assertEquals(2006, movie.getYear());
		assertEquals(443543, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void searchMovie3() throws Exception {
		List<Movie> results = imdb.searchMovie("Amélie", null);
		Movie movie = results.get(0);
		
		assertEquals("Amélie", movie.getName());
		assertEquals(2001, movie.getYear());
		assertEquals(211915, movie.getImdbId(), 0);
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
	public void getMovieDescriptor1() throws Exception {
		Movie movie = imdb.getMovieDescriptor(499549, null);
		
		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void getMovieDescriptor2() throws Exception {
		Movie movie = imdb.getMovieDescriptor(211915, null);
		
		assertEquals("Amélie", movie.getName());
		assertEquals(2001, movie.getYear());
		assertEquals(211915, movie.getImdbId(), 0);
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
