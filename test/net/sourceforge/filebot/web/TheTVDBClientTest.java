
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.TheTVDBClient.MirrorType;
import net.sourceforge.filebot.web.TheTVDBClient.TheTVDBSearchResult;


public class TheTVDBClientTest {
	
	private TheTVDBClient thetvdb = new TheTVDBClient("BA864DEE427E384A");
	

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = thetvdb.search("babylon 5");
		
		assertEquals(1, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		assertEquals("Babylon 5", first.getName());
		assertEquals(70726, first.getSeriesId());
	}
	

	@Test
	public void searchGerman() throws Exception {
		List<SearchResult> results = thetvdb.search("buffy", Locale.GERMAN);
		
		assertEquals(4, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		assertEquals("Buffy", first.getName());
		assertEquals(70327, first.getSeriesId());
		
		TheTVDBSearchResult second = (TheTVDBSearchResult) results.get(1);
		
		assertEquals("Buffy the Vampire Slayer", second.getName());
		assertEquals(70327, second.getSeriesId());
	}
	

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327));
		
		assertTrue(list.size() >= 144);
		
		// check ordinary episode
		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode());
		assertEquals("1", first.getSeason());
		
		// check special episode
		Episode last = list.get(list.size() - 1);
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Unaired Pilot", last.getTitle());
		assertEquals("Special 1", last.getEpisode());
		assertEquals("1", last.getSeason());
	}
	

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Wonderfalls", 78845), 1);
		
		assertEquals(13, list.size());
		
		Episode chosen = list.get(0);
		
		assertEquals("Wonderfalls", chosen.getSeriesName());
		assertEquals("Wax Lion", chosen.getTitle());
		assertEquals("1", chosen.getEpisode());
		assertEquals("1", chosen.getSeason());
	}
	

	@Test
	public void getEpisodeListLink() {
		assertEquals("http://www.thetvdb.com/?tab=seasonall&id=78874", thetvdb.getEpisodeListLink(new TheTVDBSearchResult("Firefly", 78874)).toString());
	}
	

	@Test
	public void getEpisodeListLinkSingleSeason() {
		assertEquals("http://www.thetvdb.com/?tab=season&seriesid=73965&seasonid=6749", thetvdb.getEpisodeListLink(new TheTVDBSearchResult("Roswell", 73965), 3).toString());
	}
	

	@Test
	public void getMirror() throws Exception {
		assertNotNull(thetvdb.getMirror(MirrorType.XML));
		assertNotNull(thetvdb.getMirror(MirrorType.BANNER));
		assertNotNull(thetvdb.getMirror(MirrorType.ZIP));
	}
	

	@Test
	public void resolveTypeMask() {
		// no flags set
		assertEquals(EnumSet.noneOf(MirrorType.class), MirrorType.fromTypeMask(0));
		
		// xml and zip flags set
		assertEquals(EnumSet.of(MirrorType.ZIP, MirrorType.XML), MirrorType.fromTypeMask(5));
		
		// all flags set
		assertEquals(EnumSet.allOf(MirrorType.class), MirrorType.fromTypeMask(7));
	}
	

	@BeforeClass
	@AfterClass
	public static void clearCache() {
		CacheManager.getInstance().clearAll();
	}
	
}
