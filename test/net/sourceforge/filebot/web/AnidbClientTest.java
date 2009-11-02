
package net.sourceforge.filebot.web;


import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.AnidbClient.AnidbSearchResult;


public class AnidbClientTest {
	
	/**
	 * 74 episodes
	 */
	private static AnidbSearchResult monsterSearchResult;
	
	/**
	 * 45 episodes
	 */
	private static AnidbSearchResult twelvekingdomsSearchResult;
	
	/**
	 * 38 episodes, lots of special characters
	 */
	private static AnidbSearchResult princessTutuSearchResult;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		monsterSearchResult = new AnidbSearchResult(1539, "Monster", null);
		twelvekingdomsSearchResult = new AnidbSearchResult(26, "Juuni Kokuki", "The Twelve Kingdoms");
		princessTutuSearchResult = new AnidbSearchResult(516, "Princess Tutu", null);
	}
	

	private AnidbClient anidb = new AnidbClient();
	

	@Test
	public void search() throws Exception {
		List<SearchResult> results = anidb.search("one piece");
		
		AnidbSearchResult result = (AnidbSearchResult) results.get(0);
		
		assertEquals("One Piece", result.getName());
		assertEquals(69, result.getAnimeId());
	}
	

	@Test
	public void searchNoMatch() throws Exception {
		List<SearchResult> results = anidb.search("i will not find anything for this query string");
		
		assertTrue(results.isEmpty());
	}
	

	@Test
	public void searchTitleAlias() throws Exception {
		// Seikai no Senki (main title), Banner of the Stars (official English title)
		assertEquals("Seikai no Senki", anidb.search("banner of the stars").get(0).getName());
		assertEquals("Seikai no Senki", anidb.search("seikai no senki").get(0).getName());
		
		// no matching title
		assertEquals("Naruto", anidb.search("naruto").get(0).getName());
	}
	

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = anidb.getEpisodeList(monsterSearchResult);
		
		assertEquals(74, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Monster", first.getSeriesName());
		assertEquals("Herr Dr. Tenma", first.getTitle());
		assertEquals("1", first.getEpisode());
		assertEquals(null, first.getSeason());
	}
	

	@Test
	public void getEpisodeListAllShortLink() throws Exception {
		List<Episode> list = anidb.getEpisodeList(twelvekingdomsSearchResult);
		
		assertEquals(45, list.size());
		
		Episode first = list.get(0);
		
		assertEquals("Juuni Kokuki", first.getSeriesName());
		assertEquals("Shadow of the Moon, The Sea of Shadow - Chapter 1", first.getTitle());
		assertEquals("1", first.getEpisode());
		assertEquals(null, first.getSeason());
	}
	

	@Test
	public void getEpisodeListEncoding() throws Exception {
		assertEquals("Raven Princess - An der schönen blauen Donau", anidb.getEpisodeList(princessTutuSearchResult).get(6).getTitle());
	}
	

	@Test
	public void getEpisodeListTrimRecap() throws Exception {
		assertEquals("Sea God of the East, Azure Sea of the West - Transition Chapter", anidb.getEpisodeList(twelvekingdomsSearchResult).get(44).getTitle());
	}
	

	@Test
	public void getEpisodeListLink() throws Exception {
		assertEquals("http://anidb.net/a1539", anidb.getEpisodeListLink(monsterSearchResult).toURL().toString());
	}
	

	@BeforeClass
	@AfterClass
	public static void clearCache() {
		CacheManager.getInstance().clearAll();
	}
	
}
