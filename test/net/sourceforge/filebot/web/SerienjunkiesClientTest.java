
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.SerienjunkiesClient.SerienjunkiesSearchResult;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class SerienjunkiesClientTest {
	
	private SerienjunkiesClient serienjunkies = new SerienjunkiesClient("9fbhw9uebfiwvbefzuwv");
	
	
	@Test
	public void search() throws Exception {
		List<SearchResult> results = serienjunkies.search("alias die agentin");
		assertEquals(1, results.size());
		
		SerienjunkiesSearchResult series = (SerienjunkiesSearchResult) results.get(0);
		assertEquals(34, series.getSeriesId());
		assertEquals("Alias", series.getLink());
		assertEquals("Alias - Die Agentin", series.getName());
		assertEquals("Alias", series.getMainTitle());
		assertEquals("Alias - Die Agentin", series.getGermanTitle());
		assertEquals("2001-09-30", series.getStartDate().toString());
	}
	
	
	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = serienjunkies.getEpisodeList(new SerienjunkiesSearchResult(260, "greys-anatomy", "Grey's Anatomy", null, null), null, Locale.GERMAN);
		
		// check ordinary episode
		Episode eps = list.get(0);
		assertEquals("Grey's Anatomy", eps.getSeriesName());
		assertEquals("Nur 48 Stunden", eps.getTitle());
		assertEquals("1", eps.getEpisode().toString());
		assertEquals("1", eps.getSeason().toString());
		assertEquals("1", eps.getAbsolute().toString());
		assertEquals("2005-03-27", eps.airdate().toString());
		
		// check umlaut in title
		eps = list.get(2);
		assertEquals("Überleben ist alles", eps.getTitle());
		assertEquals("1", eps.getSeason().toString());
		assertEquals("3", eps.getEpisode().toString());
		assertEquals("3", eps.getAbsolute().toString());
		assertEquals("2005-04-10", eps.airdate().toString());
	}
	
	
	@BeforeClass
	@AfterClass
	public static void clearCache() {
		CacheManager.getInstance().clearAll();
	}
	
}
