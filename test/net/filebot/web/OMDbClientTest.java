package net.filebot.web;

import static org.junit.Assert.*;

import java.util.List;

import net.filebot.web.TMDbClient.MovieInfo;

import org.junit.Test;

public class OMDbClientTest {

	private final OMDbClient client = new OMDbClient();

	@Test
	public void searchMovie1() throws Exception {
		List<Movie> results = client.searchMovie("Avatar", null);
		Movie movie = results.get(0);

		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}

	@Test
	public void searchMovie2() throws Exception {
		List<Movie> results = client.searchMovie("The Illusionist", null);
		Movie movie = results.get(0);

		assertEquals("The Illusionist", movie.getName());
		assertEquals(2006, movie.getYear());
		assertEquals(443543, movie.getImdbId(), 0);
	}

	@Test
	public void searchMovie3() throws Exception {
		List<Movie> results = client.searchMovie("Amélie", null);
		Movie movie = results.get(0);

		assertEquals("Amélie", movie.getName());
		assertEquals(2001, movie.getYear());
		assertEquals(211915, movie.getImdbId(), 0);
	}

	@Test
	public void searchMovie4() throws Exception {
		List<Movie> results = client.searchMovie("Heat", null);
		Movie movie = results.get(0);

		assertEquals("Heat", movie.getName());
		assertEquals(1995, movie.getYear());
		assertEquals(113277, movie.getImdbId(), 0);
	}

	@Test
	public void searchMovie6() throws Exception {
		List<Movie> results = client.searchMovie("Drive 2011", null);
		Movie movie = results.get(0);

		assertEquals("Drive", movie.getName());
		assertEquals(2011, movie.getYear());
		assertEquals(780504, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor1() throws Exception {
		Movie movie = client.getMovieDescriptor(new Movie(null, 0, 499549, -1), null);

		assertEquals("Avatar", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(499549, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor2() throws Exception {
		Movie movie = client.getMovieDescriptor(new Movie(null, 0, 211915, -1), null);

		assertEquals("Amélie", movie.getName());
		assertEquals(2001, movie.getYear());
		assertEquals(211915, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor3() throws Exception {
		Movie movie = client.getMovieDescriptor(new Movie(null, 0, 75610, -1), null);

		assertEquals("21 Up", movie.getName());
		assertEquals(1977, movie.getYear());
		assertEquals(75610, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor4() throws Exception {
		Movie movie = client.getMovieDescriptor(new Movie(null, 0, 369702, -1), null);

		assertEquals("The Sea Inside", movie.getName());
		assertEquals(2004, movie.getYear());
		assertEquals(369702, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor5() throws Exception {
		Movie movie = client.getMovieDescriptor(new Movie(null, 0, 1020960, -1), null);

		assertEquals("God, the Universe and Everything Else", movie.getName());
		assertEquals(1988, movie.getYear());
		assertEquals(1020960, movie.getImdbId(), 0);
	}

	@Test
	public void getImdbApiMovieInfoReleasedNA() throws Exception {
		MovieInfo movie = client.getMovieInfo(new Movie(null, -1, 1287357, -1));
		assertEquals("Sommersonntag", movie.getName());
		assertEquals(2008, movie.getReleased().getYear());
		assertEquals("2008-06-07", movie.getReleased().toString());
	}

}
