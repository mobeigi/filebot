package net.sourceforge.filebot.web;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import net.sourceforge.filebot.web.TMDbClient.MovieInfo;

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
	public void searchMovie4() throws Exception {
		List<Movie> results = imdb.searchMovie("Heat", null);
		Movie movie = results.get(0);

		assertEquals("Heat", movie.getName());
		assertEquals(1995, movie.getYear());
		assertEquals(113277, movie.getImdbId(), 0);
	}

	@Test
	public void searchMovie5() throws Exception {
		List<Movie> results = imdb.searchMovie("Det sjunde inseglet", null);
		Movie movie = results.get(0);

		assertEquals("The Seventh Seal", movie.getName());
		assertEquals(1957, movie.getYear());
		assertEquals(50976, movie.getImdbId(), 0);
	}

	@Test
	public void searchMovie6() throws Exception {
		List<Movie> results = imdb.searchMovie("Drive 2011", null);
		Movie movie = results.get(0);

		assertEquals("Drive", movie.getName());
		assertEquals(2011, movie.getYear());
		assertEquals(780504, movie.getImdbId(), 0);
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
	public void getMovieDescriptor3() throws Exception {
		Movie movie = imdb.getMovieDescriptor(75610, null);

		assertEquals("21", movie.getName());
		assertEquals(1977, movie.getYear());
		assertEquals(75610, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor4() throws Exception {
		Movie movie = imdb.getMovieDescriptor(369702, null);

		assertEquals("The Sea Inside", movie.getName());
		assertEquals(2004, movie.getYear());
		assertEquals(369702, movie.getImdbId(), 0);
	}

	@Test
	public void getMovieDescriptor5() throws Exception {
		Movie movie = imdb.getMovieDescriptor(1020960, null);

		assertEquals("God, the Universe and Everything Else", movie.getName());
		assertEquals(1988, movie.getYear());
		assertEquals(1020960, movie.getImdbId(), 0);
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

	@Test
	public void getImdbApiMovieInfoReleasedNA() throws Exception {
		MovieInfo movie = imdb.getImdbApiMovieInfo(new Movie(null, -1, 1287357, -1));
		assertEquals("Sommersonntag", movie.getName());
		assertEquals(2008, movie.getReleased().getYear());
		assertEquals("2008-01-01", movie.getReleased().toString());
	}

}
