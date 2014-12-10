package net.filebot.web;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import net.sf.ehcache.CacheManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
		twelvekingdomsSearchResult = new AnidbSearchResult(26, "Juuni Kokuki", null);
		princessTutuSearchResult = new AnidbSearchResult(516, "Princess Tutu", null);
	}

	private AnidbClient anidb = new AnidbClient("filebot", 6);

	@Test
	public void getAnimeTitles() throws Exception {
		List<AnidbSearchResult> animeTitles = anidb.getAnimeTitles();
		assertTrue(animeTitles.size() > 8000);
	}

	@Test
	public void search() throws Exception {
		List<SearchResult> results = anidb.search("one piece", Locale.ENGLISH);

		AnidbSearchResult result = (AnidbSearchResult) results.get(0);
		assertEquals("One Piece", result.getName());
		assertEquals(69, result.getAnimeId());
	}

	@Test
	public void searchNoMatch() throws Exception {
		List<SearchResult> results = anidb.search("i will not find anything for this query string", Locale.ENGLISH);

		assertTrue(results.isEmpty());
	}

	@Test
	public void searchTitleAlias() throws Exception {
		// Seikai no Senki (main title), Banner of the Stars (official English title)
		assertEquals("Seikai no Senki", anidb.search("banner of the stars", Locale.ENGLISH).get(0).getName());
		assertEquals("Seikai no Senki", anidb.search("seikai no senki", Locale.ENGLISH).get(0).getName());

		// no matching title
		assertEquals("Naruto", anidb.search("naruto", Locale.ENGLISH).get(0).getName());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = anidb.getEpisodeList(monsterSearchResult, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(77, list.size());

		Episode first = list.get(0);

		assertEquals("Monster", first.getSeriesName());
		assertEquals("2004-04-07", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Herr Dr. Tenma", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals(null, first.getSeason());
		assertEquals("2004-04-07", first.getAirdate().toString());
	}

	@Test
	public void getEpisodeListAllShortLink() throws Exception {
		List<Episode> list = anidb.getEpisodeList(twelvekingdomsSearchResult, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(46, list.size());

		Episode first = list.get(0);

		assertEquals("The Twelve Kingdoms", first.getSeriesName());
		assertEquals("2002-04-09", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Shadow of the Moon, The Sea of Shadow - Chapter 1", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals(null, first.getSeason());
		assertEquals("2002-04-09", first.getAirdate().toString());
	}

	@Test
	public void getEpisodeListEncoding() throws Exception {
		assertEquals("Raven Princess - An der schönen blauen Donau", anidb.getEpisodeList(princessTutuSearchResult, SortOrder.Airdate, Locale.ENGLISH).get(6).getTitle());
	}

	@Test
	public void getEpisodeListI18N() throws Exception {
		List<Episode> list = anidb.getEpisodeList(monsterSearchResult, SortOrder.Airdate, Locale.JAPANESE);

		Episode last = list.get(73);
		assertEquals("モンスター", last.getSeriesName());
		assertEquals("2004-04-07", last.getSeriesInfo().getStartDate().toString());
		assertEquals("本当の怪物", last.getTitle());
		assertEquals("74", last.getEpisode().toString());
		assertEquals("74", last.getAbsolute().toString());
		assertEquals(null, last.getSeason());
		assertEquals("2005-09-28", last.getAirdate().toString());
	}

	@Test
	public void getEpisodeListTrimRecap() throws Exception {
		assertEquals("Sea God of the East, Azure Sea of the West - Transition Chapter", anidb.getEpisodeList(twelvekingdomsSearchResult, SortOrder.Airdate, Locale.ENGLISH).get(44).getTitle());
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
