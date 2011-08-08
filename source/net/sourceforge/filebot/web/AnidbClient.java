
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeListUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;


public class AnidbClient extends AbstractEpisodeListProvider {
	
	private static final String host = "anidb.net";
	private static final AnidbCache cache = new AnidbCache(CacheManager.getInstance().getCache("web-persistent-datasource"));
	
	private final String client;
	private final int clientver;
	

	public AnidbClient(String client, int clientver) {
		this.client = client;
		this.clientver = clientver;
	}
	

	@Override
	public String getName() {
		return "AniDB";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.anidb");
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return false;
	}
	

	@Override
	public boolean hasLocaleSupport() {
		return true;
	}
	

	@Override
	public List<SearchResult> search(String query, Locale locale) throws Exception {
		// normalize
		query = query.toLowerCase();
		
		AbstractStringMetric metric = new QGramsDistance();
		
		final List<Entry<SearchResult, Float>> resultSet = new ArrayList<Entry<SearchResult, Float>>();
		
		for (AnidbSearchResult anime : getAnimeTitles()) {
			for (String name : new String[] { anime.getMainTitle(), anime.getEnglishTitle() }) {
				if (name != null) {
					// normalize
					name = name.toLowerCase();
					
					float similarity = metric.getSimilarity(name, query);
					
					if (similarity > 0.5 || name.contains(query)) {
						resultSet.add(new SimpleEntry<SearchResult, Float>(anime, similarity));
						
						// add only once
						break;
					}
				}
			}
		}
		
		// sort by similarity descending (best matches first)
		Collections.sort(resultSet, new Comparator<Entry<SearchResult, Float>>() {
			
			@Override
			public int compare(Entry<SearchResult, Float> o1, Entry<SearchResult, Float> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		
		// view for the first 20 search results
		return new AbstractList<SearchResult>() {
			
			@Override
			public SearchResult get(int index) {
				return resultSet.get(index).getKey();
			}
			

			@Override
			public int size() {
				return Math.min(20, resultSet.size());
			}
		};
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, Locale language) throws Exception {
		AnidbSearchResult anime = (AnidbSearchResult) searchResult;
		
		// e.g. http://api.anidb.net:9001/httpapi?request=anime&client=filebot&clientver=1&protover=1&aid=4521
		URL url = new URL("http", "api." + host, 9001, "/httpapi?request=anime&client=" + client + "&clientver=" + clientver + "&protover=1&aid=" + anime.getAnimeId());
		
		// try cache first
		List<Episode> episodes = cache.getEpisodeList(anime.getAnimeId(), language.getLanguage());
		if (episodes != null)
			return episodes;
		
		// get anime page as xml
		Document dom = getDocument(url);
		
		// select main title
		String animeTitle = selectString("//titles/title[@type='official' and @lang='" + language.getLanguage() + "']", dom);
		if (animeTitle.isEmpty()) {
			animeTitle = selectString("//titles/title[@type='main']", dom);
		}
		
		episodes = new ArrayList<Episode>(25);
		
		for (Node node : selectNodes("//episode", dom)) {
			Integer number = getIntegerContent("epno", node);
			
			// ignore special episodes
			if (number != null) {
				String title = selectString(".//title[@lang='" + language.getLanguage() + "']", node);
				if (title.isEmpty()) { // English language fall-back
					title = selectString(".//title[@lang='en']", node);
				}
				
				String airdate = getTextContent("airdate", node);
				
				// no seasons for anime
				episodes.add(new Episode(animeTitle, null, number, title, number, null, Date.parse(airdate, "yyyy-MM-dd")));
			}
		}
		
		// make sure episodes are in ordered correctly
		sortEpisodes(episodes);
		
		// sanity check 
		if (episodes.size() > 0) {
			// populate cache
			cache.putEpisodeList(episodes, anime.getAnimeId(), language.getLanguage());
		} else {
			// anime page xml doesn't work sometimes
			throw new RuntimeException(String.format("Failed to parse episode data from xml: %s (%d)", anime, anime.getAnimeId()));
		}
		
		return episodes;
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		int aid = ((AnidbSearchResult) searchResult).getAnimeId();
		
		try {
			return new URI("http", host, "/a" + aid, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return null;
	}
	

	protected List<AnidbSearchResult> getAnimeTitles() throws Exception {
		URL url = new URL("http", host, "/api/animetitles.dat.gz");
		
		// try cache first
		List<AnidbSearchResult> anime = cache.getAnimeList();
		if (anime != null)
			return anime;
		
		// <aid>|<type>|<language>|<title>
		// type: 1=primary title (one per anime), 2=synonyms (multiple per anime), 3=shorttitles (multiple per anime), 4=official title (one per language)
		Pattern pattern = Pattern.compile("^(?!#)(\\d+)[|](\\d)[|]([\\w-]+)[|](.+)$");
		
		Map<Integer, String> primaryTitleMap = new TreeMap<Integer, String>();
		Map<Integer, String> englishTitleMap = new HashMap<Integer, String>();
		
		// fetch data
		Scanner scanner = new Scanner(new GZIPInputStream(url.openStream()), "UTF-8");
		
		try {
			while (scanner.hasNextLine()) {
				Matcher matcher = pattern.matcher(scanner.nextLine());
				
				if (matcher.matches()) {
					if (matcher.group(2).equals("1")) {
						primaryTitleMap.put(Integer.parseInt(matcher.group(1)), matcher.group(4));
					} else if (matcher.group(2).equals("4") && matcher.group(3).equals("en")) {
						englishTitleMap.put(Integer.parseInt(matcher.group(1)), matcher.group(4));
					}
				}
			}
		} finally {
			scanner.close();
		}
		
		// build up a list of all possible anidb search results
		anime = new ArrayList<AnidbSearchResult>(primaryTitleMap.size());
		
		for (Entry<Integer, String> entry : primaryTitleMap.entrySet()) {
			anime.add(new AnidbSearchResult(entry.getKey(), entry.getValue(), englishTitleMap.get(entry.getKey())));
		}
		
		// populate cache
		cache.putAnimeList(anime);
		
		return anime;
	}
	

	public static class AnidbSearchResult extends SearchResult implements Serializable {
		
		protected int aid;
		protected String mainTitle;
		protected String englishTitle;
		

		protected AnidbSearchResult() {
			// used by serializer
		}
		

		public AnidbSearchResult(int aid, String mainTitle, String englishTitle) {
			this.aid = aid;
			this.mainTitle = mainTitle;
			this.englishTitle = englishTitle;
		}
		

		public int getAnimeId() {
			return aid;
		}
		

		@Override
		public String getName() {
			return mainTitle;
		}
		

		public String getMainTitle() {
			return mainTitle;
		}
		

		public String getEnglishTitle() {
			return englishTitle;
		}
	}
	

	private static class AnidbCache {
		
		private final Cache cache;
		

		public AnidbCache(Cache cache) {
			this.cache = cache;
		}
		

		public void putAnimeList(Collection<AnidbSearchResult> anime) {
			cache.put(new Element(host + "AnimeList", anime.toArray(new AnidbSearchResult[0])));
		}
		

		public List<AnidbSearchResult> getAnimeList() {
			Element element = cache.get(host + "AnimeList");
			
			if (element != null)
				return Arrays.asList((AnidbSearchResult[]) element.getValue());
			
			return null;
		}
		

		public void putEpisodeList(Collection<Episode> episodes, int aid, String lang) {
			cache.put(new Element(host + "EpisodeList" + aid + lang, episodes.toArray(new Episode[0])));
		}
		

		public List<Episode> getEpisodeList(int aid, String lang) {
			Element element = cache.get(host + "EpisodeList" + aid + lang);
			
			if (element != null)
				return Arrays.asList((Episode[]) element.getValue());
			
			return null;
		}
		
	}
	
}
