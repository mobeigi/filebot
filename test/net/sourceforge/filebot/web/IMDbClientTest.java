
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;


public class IMDbClientTest {
	
	private final IMDbClient imdb = new IMDbClient();
	

	@Test
	public void search() throws Exception {
		List<SearchResult> results = imdb.search("battlestar");
		
		Movie movie = (Movie) results.get(0);
		
		assertEquals("Battlestar Galactica", movie.getName());
		assertEquals(2004, movie.getYear());
		assertEquals(407362, movie.getImdbId(), 0);
		
		assertEquals(8, results.size(), 0);
	}
	

	@Test
	public void searchMiniSeries() throws Exception {
		List<SearchResult> results = imdb.search("generation kill");
		
		Movie movie = (Movie) results.get(0);
		
		assertEquals("Generation Kill", movie.getName());
		assertEquals(2008, movie.getYear());
		assertEquals(995832, movie.getImdbId(), 0);
	}
	

	@Test
	public void searchNoMatch() throws Exception {
		List<SearchResult> results = imdb.search("i will not find anything for this query string");
		
		assertTrue(results.isEmpty());
	}
	

	@Test
	public void searchResultPageRedirect() throws Exception {
		List<SearchResult> results = imdb.search("my name is earl");
		
		// exactly one search result
		assertEquals(1, results.size(), 0);
		
		Movie movie = (Movie) results.get(0);
		
		assertEquals("My Name Is Earl", movie.getName());
		assertEquals(460091, movie.getImdbId(), 0);
	}
	

	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> list = imdb.getEpisodeList(new Movie("Buffy", 1997, 118276));
		
		assertEquals(145, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-00-00", first.getSeriesStartDate().toString());
		assertEquals("Unaired Pilot", first.getTitle());
		assertEquals("0", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals(null, first.airdate());
		
		Episode last = list.get(144);
		
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("1997-00-00", first.getSeriesStartDate().toString());
		assertEquals("Chosen", last.getTitle());
		assertEquals("22", last.getEpisode().toString());
		assertEquals("7", last.getSeason().toString());
		assertEquals("2003-05-20", last.airdate().toString());
	}
	

	@Test
	public void getEpisodeListWithUnknownSeason() throws Exception {
		List<Episode> list = imdb.getEpisodeList(new Movie("Mushishi", 2005, 807832));
		
		assertEquals(26, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Mushi-Shi", first.getSeriesName());
		assertEquals("2005-00-00", first.getSeriesStartDate().toString());
		assertEquals("Midori no za", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
	}
	

	@Test
	public void getEpisodeListLink() throws Exception {
		assertEquals("http://www.imdb.com/title/tt0407362/episodes", imdb.getEpisodeListLink(new Movie("Battlestar Galactica", 2004, 407362)).toString());
	}
	
}
