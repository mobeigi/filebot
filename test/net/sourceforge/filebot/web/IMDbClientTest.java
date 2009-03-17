
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;


public class IMDbClientTest {
	
	private final IMDbClient imdb = new IMDbClient();
	
	
	@Test
	public void search() throws Exception {
		List<SearchResult> results = imdb.search("battlestar");
		
		MovieDescriptor movie = (MovieDescriptor) results.get(0);
		
		assertEquals("Battlestar Galactica (2004)", movie.getName());
		assertEquals(407362, movie.getImdbId(), 0);
		
		assertEquals(6, results.size(), 0);
	}
	

	@Test
	public void getEpisodeList() throws Exception {
		List<Episode> list = imdb.getEpisodeList(new MovieDescriptor("Buffy", 118276));
		
		assertEquals(145, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("Unaired Pilot", first.getTitle());
		assertEquals("0", first.getEpisodeNumber());
		assertEquals("1", first.getSeasonNumber());
		
		Episode last = list.get(144);
		
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Chosen", last.getTitle());
		assertEquals("22", last.getEpisodeNumber());
		assertEquals("7", last.getSeasonNumber());
	}
	

	@Test
	public void getEpisodeListWithUnknownSeason() throws Exception {
		List<Episode> list = imdb.getEpisodeList(new MovieDescriptor("Mushishi", 807832));
		
		assertEquals(26, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Mushishi", first.getSeriesName());
		assertEquals("Midori no za", first.getTitle());
		assertEquals("1", first.getEpisodeNumber());
		assertEquals("1", first.getSeasonNumber());
	}
	

	@Test
	public void getEpisodeListLink() throws Exception {
		assertEquals("http://www.imdb.com/title/tt0407362/episodes", imdb.getEpisodeListLink(new MovieDescriptor("Battlestar Galactica", 407362)).toString());
	}
	

	@Test
	public void removeQuotationMarks() throws Exception {
		assertEquals("test", imdb.removeQuotationMarks("\"test\""));
		
		assertEquals("inner \"quotation marks\"", imdb.removeQuotationMarks("\"inner \"quotation marks\"\""));
	}
}
