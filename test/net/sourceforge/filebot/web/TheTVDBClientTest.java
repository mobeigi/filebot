
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.TheTVDBClient.BannerDescriptor;
import net.sourceforge.filebot.web.TheTVDBClient.MirrorType;
import net.sourceforge.filebot.web.TheTVDBClient.SeriesInfo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


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
		List<SearchResult> results = thetvdb.search("Buffy the Vampire Slayer", Locale.GERMAN);
		
		assertEquals(2, results.size());
		
		TheTVDBSearchResult first = (TheTVDBSearchResult) results.get(0);
		
		assertEquals("Buffy the Vampire Slayer", first.getName());
		assertEquals(70327, first.getSeriesId());
	}
	
	
	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327));
		
		assertTrue(list.size() >= 144);
		
		// check ordinary episode
		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-03-10", first.getSeriesStartDate().toString());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("1997-03-10", first.getAirdate().toString());
		
		// check special episode
		Episode last = list.get(list.size() - 1);
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Unaired Pilot", last.getTitle());
		assertEquals("1", last.getSeason().toString());
		assertEquals(null, last.getEpisode());
		assertEquals(null, last.getAbsolute());
		assertEquals("1", last.getSpecial().toString());
		assertEquals(null, last.getAirdate());
	}
	
	
	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Wonderfalls", 78845));
		
		Episode first = list.get(0);
		
		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("2004-03-12", first.getSeriesStartDate().toString());
		assertEquals("Wax Lion", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals(null, first.getAbsolute()); // should be "1" but data has not yet been entered
		assertEquals("2004-03-12", first.getAirdate().toString());
	}
	
	
	@Test
	public void getEpisodeListNumbering() throws Exception {
		List<Episode> list = thetvdb.getEpisodeList(new TheTVDBSearchResult("Firefly", 78874), SortOrder.DVD, Locale.ENGLISH);
		
		Episode first = list.get(0);
		assertEquals("Firefly", first.getSeriesName());
		assertEquals("2002-09-20", first.getSeriesStartDate().toString());
		assertEquals("Serenity", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("2002-12-20", first.getAirdate().toString());
	}
	
	
	@Test
	public void getEpisodeListLink() {
		assertEquals("http://www.thetvdb.com/?tab=seasonall&id=78874", thetvdb.getEpisodeListLink(new TheTVDBSearchResult("Firefly", 78874)).toString());
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
		assertEquals(EnumSet.of(MirrorType.ZIP, MirrorType.XML, MirrorType.SEARCH), MirrorType.fromTypeMask(5));
		
		// all flags set
		assertEquals(EnumSet.allOf(MirrorType.class), MirrorType.fromTypeMask(7));
	}
	
	
	@Test
	public void lookupByID() throws Exception {
		TheTVDBSearchResult series = thetvdb.lookupByID(78874, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getSeriesId());
	}
	
	
	@Test
	public void lookupByIMDbID() throws Exception {
		TheTVDBSearchResult series = thetvdb.lookupByIMDbID(303461, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getSeriesId());
	}
	
	
	@Test
	public void getSeriesInfo() throws Exception {
		SeriesInfo it = thetvdb.getSeriesInfo(new TheTVDBSearchResult(null, 80348), Locale.ENGLISH);
		
		assertEquals(80348, it.getId(), 0);
		assertEquals("TV-PG", it.getContentRating());
		assertEquals("2007-09-24", it.getFirstAired().toString());
		assertEquals("Action", it.getGenres().get(0));
		assertEquals(934814, it.getImdbId(), 0);
		assertEquals("English", it.getLanguage().getDisplayLanguage(Locale.ENGLISH));
		assertEquals(310, it.getOverview().length());
		assertEquals("60", it.getRuntime());
		assertEquals("Chuck", it.getName());
	}
	
	
	@Test
	public void getBanner() throws Exception {
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("BannerType", "season");
		filter.put("BannerType2", "seasonwide");
		filter.put("Season", "7");
		filter.put("Language", "en");
		
		BannerDescriptor banner = thetvdb.getBanner(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327), filter);
		
		assertEquals(857660, banner.getId(), 0);
		assertEquals("season", banner.getBannerType());
		assertEquals("seasonwide", banner.getBannerType2());
		assertEquals("http://thetvdb.com/banners/seasonswide/70327-7.jpg", banner.getUrl().toString());
		assertEquals(99712, WebRequest.fetch(banner.getUrl()).remaining(), 0);
	}
	
	
	@Test
	public void getBannerList() throws Exception {
		List<BannerDescriptor> banners = thetvdb.getBannerList(new TheTVDBSearchResult("Buffy the Vampire Slayer", 70327));
		
		assertEquals("fanart", banners.get(0).getBannerType());
		assertEquals("1280x720", banners.get(0).getBannerType2());
		assertEquals(486993, WebRequest.fetch(banners.get(0).getUrl()).remaining(), 0);
	}
	
	
	@BeforeClass
	@AfterClass
	public static void clearCache() {
		CacheManager.getInstance().clearAll();
	}
	
}
