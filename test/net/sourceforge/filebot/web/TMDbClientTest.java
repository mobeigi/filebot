
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.Settings.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

import net.sourceforge.filebot.web.TMDbClient.MovieInfo;


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
		assertEquals(2005, movie.getYear(), 0);
		assertEquals(401792, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void searchByIMDB() throws Exception {
		Movie movie = tmdb.getMovieDescriptor(418279, Locale.ENGLISH);
		
		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear(), 0);
		assertEquals(418279, movie.getImdbId(), 0);
	}
	
	
	@Test
	public void getMovieInfo() throws Exception {
		MovieInfo movie = tmdb.getMovieInfo(new Movie(null, 0, 418279), Locale.ENGLISH);
		
		assertEquals("Transformers", movie.getName());
		assertEquals("2007-07-03", movie.getReleased().toString());
		assertEquals("Adventure", movie.getGenres().get(0));
		assertEquals("Deborah Lynn Scott", movie.getCast().get(0).getName());
		assertEquals("Costume Design", movie.getCast().get(0).getJob());
		assertEquals("thumb", movie.getImages().get(0).getSize());
		assertEquals("http://cf2.imgobject.com/t/p/w92/bgSHbGEA1OM6qDs3Qba4VlSZsNG.jpg", movie.getImages().get(0).getUrl().toString());
	}
	
	
	@Test
	public void floodLimit() throws Exception {
		for (Locale it : Locale.getAvailableLocales()) {
			List<Movie> results = tmdb.searchMovie("Serenity", it);
			assertEquals(379786, results.get(0).getImdbId());
		}
	}
	
}
