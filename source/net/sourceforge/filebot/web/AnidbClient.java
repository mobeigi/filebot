
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;


public class AnidbClient implements EpisodeListProvider {
	
	private static final String host = "anidb.net";
	
	private static final Cache cache = CacheManager.getInstance().getCache("anidb");
	

	@Override
	public String getName() {
		return "AniDB";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.anidb");
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		// normalize
		query = query.toLowerCase();
		
		AbstractStringMetric metric = new QGramsDistance();
		
		final List<Entry<SearchResult, Float>> resultSet = new ArrayList<Entry<SearchResult, Float>>();
		
		for (AnidbSearchResult anime : getAnimeTitles()) {
			for (String name : new String[] { anime.getMainTitle(), anime.getEnglishTitle() }) {
				if (name != null) {
					float similarity = metric.getSimilarity(name.toLowerCase(), query);
					
					if (similarity > 0.5 || name.toLowerCase().contains(query)) {
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
	public List<Episode> getEpisodeList(SearchResult searchResult) throws IOException, SAXException {
		int aid = ((AnidbSearchResult) searchResult).getAnimeId();
		URL url = new URL("http", host, "/perl-bin/animedb.pl?show=xml&t=anime&aid=" + aid);
		
		// try cache first
		try {
			return Arrays.asList((Episode[]) cache.get(url.toString()).getValue());
		} catch (Exception e) {
			// ignore
		}
		
		// get anime page as xml
		Document dom = getDocument(url);
		
		// select main title
		String animeTitle = selectString("//title[@type='main']", dom);
		
		List<Episode> episodes = new ArrayList<Episode>(25);
		
		for (Node node : selectNodes("//ep", dom)) {
			String flags = getTextContent("flags", node);
			
			// allow only normal and recap episodes
			if (flags == null || flags.equals("2")) {
				String number = getTextContent("epno", node);
				String title = selectString(".//title[@lang='en']", node);
				
				// no seasons for anime
				episodes.add(new Episode(animeTitle, null, number, title));
			}
		}
		
		// sanity check 
		if (episodes.size() > 0) {
			// populate cache
			cache.put(new Element(url.toString(), episodes.toArray(new Episode[0])));
		} else {
			// anime page xml doesn't work sometimes
			Logger.getLogger(getClass().getName()).warning(String.format("Failed to parse episode data from xml: %s (%d)", searchResult, aid));
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
	public boolean hasSingleSeasonSupport() {
		return false;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return null;
	}
	

	private AnidbSearchResult[] getAnimeTitles() throws MalformedURLException, IOException, SAXException {
		URL url = new URL("http", host, "/api/animetitles.dat.gz");
		
		// try cache first
		try {
			return (AnidbSearchResult[]) cache.get(url.toString()).getValue();
		} catch (Exception e) {
			// ignore
		}
		
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
		
		List<AnidbSearchResult> anime = new ArrayList<AnidbSearchResult>(primaryTitleMap.size());
		
		for (Entry<Integer, String> entry : primaryTitleMap.entrySet()) {
			anime.add(new AnidbSearchResult(entry.getKey(), entry.getValue(), englishTitleMap.get(entry.getKey())));
		}
		
		// populate cache
		AnidbSearchResult[] result = anime.toArray(new AnidbSearchResult[0]);
		cache.put(new Element(url.toString(), result));
		
		return result;
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
	
}
