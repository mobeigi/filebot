
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class OpenSubtitlesXmlRpcTest {
	
	private static OpenSubtitlesXmlRpc xmlrpc = new OpenSubtitlesXmlRpc("FileBot 0.0");
	

	@BeforeClass
	public static void login() throws Exception {
		// login manually
		xmlrpc.loginAnonymous();
	}
	

	@Test
	public void search() throws Exception {
		List<MovieDescriptor> list = xmlrpc.searchMoviesOnIMDB("babylon 5");
		
		MovieDescriptor sample = (MovieDescriptor) list.get(0);
		
		// check sample entry
		assertEquals("\"Babylon 5\" (1994) (TV series)", sample.getName());
		assertEquals(105946, sample.getImdbId());
	}
	

	@Test
	public void getSubtitleListEnglish() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(361256, "eng");
		
		SubtitleDescriptor sample = list.get(0);
		
		assertTrue(sample.getName().startsWith("Wonderfalls"));
		assertEquals("English", sample.getLanguageName());
		
		// check size
		assertTrue(list.size() > 20);
	}
	

	@Test
	public void getSubtitleListAllLanguages() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(361256);
		
		OpenSubtitlesSubtitleDescriptor sample = list.get(75);
		
		assertTrue(sample.getName().startsWith("Wonderfalls"));
		assertEquals("Hungarian", sample.getLanguageName());
		
		// check size
		assertTrue(list.size() > 70);
	}
	

	@Test
	public void getSubtitleListMovieHash() {
		//TODO not implemented yet
	}
	

	@Test
	public void fetchSubtitle() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(361256, "eng");
		
		// check format
		assertEquals("srt", list.get(0).getType());
		
		// fetch subtitle file
		ByteBuffer data = list.get(0).fetch();
		
		// check size
		assertEquals(48550, data.remaining(), 0);
	}
	

	@AfterClass
	public static void logout() throws Exception {
		// logout manually
		xmlrpc.logout();
	}
	
}
