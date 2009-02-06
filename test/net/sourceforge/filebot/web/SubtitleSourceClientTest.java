
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.Test;


public class SubtitleSourceClientTest {
	
	private static final SubtitleSourceClient client = new SubtitleSourceClient();
	
	
	@Test
	public void search() throws Exception {
		List<SearchResult> list = client.search("babylon 5");
		
		MovieDescriptor sample = (MovieDescriptor) list.get(0);
		
		// check sample entry
		assertEquals("Babylon 5", sample.getName());
		assertEquals(105946, sample.getImdbId());
		
		// check page size
		assertEquals(1, list.size());
	}
	

	@Test
	public void getSubtitleListAll() throws Exception {
		List<SubtitleDescriptor> list = client.getSubtitleList(new MovieDescriptor("Buffy", 118276), Locale.ENGLISH);
		
		SubtitleDescriptor sample = list.get(0);
		
		// check sample entry (order is unpredictable)
		assertTrue(sample.getName().startsWith("Buffy"));
		assertEquals("English", sample.getLanguageName());
		
		// check size
		assertTrue(list.size() > 100);
	}
	

	@Test
	public void getSubtitleListSinglePage() throws Exception {
		List<SubtitleDescriptor> list = client.getSubtitleList(new MovieDescriptor("Firefly", 303461), 0);
		
		SubtitleDescriptor sample = list.get(0);
		
		// check sample entry (order is unpredictable)
		assertTrue(sample.getName().startsWith("Firefly"));
		
		// check page size
		assertEquals(20, list.size());
	}
	

	@Test
	public void getSubtitleListLink() {
		assertEquals("http://www.subtitlesource.org/title/tt0303461", client.getSubtitleListLink(new MovieDescriptor("Firefly", 303461), null).toString());
	}
}
