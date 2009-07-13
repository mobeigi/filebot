
package net.sourceforge.filebot.web;


import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sourceforge.filebot.web.OpenSubtitlesSubtitleDescriptor.Property;
import net.sourceforge.filebot.web.OpenSubtitlesXmlRpc.Query;


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
		
		assertEquals("\"Wonderfalls\"", sample.getProperty(Property.MovieName));
		assertEquals("Hungarian", sample.getProperty(Property.LanguageName));
		assertEquals("imdbid", sample.getProperty(Property.MatchedBy));
		
		// check size
		assertTrue(list.size() > 70);
	}
	

	@Test
	public void getSubtitleListMovieHash() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(singleton(Query.forHash("2bba5c34b007153b", 717565952, "eng")));
		
		OpenSubtitlesSubtitleDescriptor sample = list.get(0);
		
		assertEquals("firefly.s01e01.serenity.pilot.dvdrip.xvid.srt", sample.getProperty(Property.SubFileName));
		assertEquals("English", sample.getProperty(Property.LanguageName));
		assertEquals("moviehash", sample.getProperty(Property.MatchedBy));
	}
	

	@Test
	public void checkSubHash() throws Exception {
		Map<String, Integer> subHashMap = xmlrpc.checkSubHash(singleton("e12715f466ee73c86694b7ab9f311285"));
		
		assertEquals("247060", subHashMap.values().iterator().next().toString());
		assertTrue(1 == subHashMap.size());
	}
	

	@Test
	public void checkSubHashInvalid() throws Exception {
		Map<String, Integer> subHashMap = xmlrpc.checkSubHash(singleton("0123456789abcdef0123456789abcdef"));
		
		assertEquals("0", subHashMap.values().iterator().next().toString());
		assertTrue(1 == subHashMap.size());
	}
	

	@Test
	public void checkMovieHash() throws Exception {
		Map<String, Map<Property, String>> movieHashMap = xmlrpc.checkMovieHash(singleton("2bba5c34b007153b"));
		Map<Property, String> movie = movieHashMap.values().iterator().next();
		
		assertEquals("\"Firefly\"", movie.get(Property.MovieName));
		assertEquals("2002", movie.get(Property.MovieYear));
	}
	

	@Test
	public void checkMovieHashInvalid() throws Exception {
		Map<String, Map<Property, String>> movieHashMap = xmlrpc.checkMovieHash(singleton("0123456789abcdef"));
		
		// no movie info
		assertTrue(movieHashMap.isEmpty());
	}
	

	@Test
	public void detectLanguage() throws Exception {
		String text = "Only those that are prepared to fire should be fired at.";
		
		List<String> languages = xmlrpc.detectLanguage(Charset.forName("utf-8").encode(text));
		
		assertEquals("eng", languages.get(0));
		assertTrue(1 == languages.size());
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
