
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.TheTVDBClient.MirrorType;
import net.sourceforge.filebot.web.TheTVDBClient.TheTVDBSearchResult;

import org.junit.After;
import org.junit.Test;


public class TheTVDBClientTest {
	
	private TheTVDBClient thetvdb = new TheTVDBClient("BA864DEE427E384A");
	
	
	@After
	public void clearCache() {
		CacheManager.getInstance().clearAll();
	}
	

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = thetvdb.search("babylon 5");
		
		assertEquals(2, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		assertEquals("Babylon 5", first.getName());
		assertEquals(70726, first.getSeriesId());
	}
	

	@Test
	public void searchGerman() throws Exception {
		List<SearchResult> results = thetvdb.search("buffy", Locale.GERMAN);
		
		assertEquals(3, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		// test encoding (umlauts)
		assertEquals("Buffy - Im Bann der Dämonen", first.getName());
		assertEquals(70327, first.getSeriesId());
		
		TheTVDBSearchResult second = (TheTVDBSearchResult) results.get(1);
		
		assertEquals("Buffy the Vampire Slayer", second.getName());
		assertEquals(70327, second.getSeriesId());
	}
	

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327));
		
		assertTrue(list.size() >= 144);
		
		Episode first = list.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("Unaired Pilot", first.getTitle());
		assertEquals("1", first.getEpisodeNumber());
		assertEquals("0", first.getSeasonNumber());
	}
	

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327), 7);
		
		assertEquals(22, list.size());
		
		Episode chosen = list.get(21);
		
		assertEquals("Buffy the Vampire Slayer", chosen.getSeriesName());
		assertEquals("Chosen", chosen.getTitle());
		assertEquals("22", chosen.getEpisodeNumber());
		assertEquals("7", chosen.getSeasonNumber());
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
	
}
