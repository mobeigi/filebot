
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeListUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.Icon;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;


public class SerienjunkiesClient extends AbstractEpisodeListProvider {
	
	private static final String host = "api.serienjunkies.de";
	private static final SerienjunkiesCache cache = new SerienjunkiesCache(CacheManager.getInstance().getCache("web-persistent-datasource"));
	
	private final String apikey;
	

	public SerienjunkiesClient(String apikey) {
		this.apikey = apikey;
	}
	

	@Override
	public String getName() {
		return "Serienjunkies";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.serienjunkies");
	}
	

	@Override
	public List<SearchResult> search(String query, Locale locale) throws IOException {
		// normalize
		query = query.toLowerCase();
		
		AbstractStringMetric metric = new QGramsDistance();
		
		final List<Entry<SearchResult, Float>> resultSet = new ArrayList<Entry<SearchResult, Float>>();
		
		for (SerienjunkiesSearchResult anime : getSeriesTitles()) {
			for (String name : new String[] { anime.getMainTitle(), anime.getGermanTitle() }) {
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
	

	protected List<SerienjunkiesSearchResult> getSeriesTitles() throws IOException {
		// try cache first
		List<SerienjunkiesSearchResult> seriesList = cache.getSeriesList();
		if (seriesList != null)
			return seriesList;
		
		// fetch series data
		seriesList = new ArrayList<SerienjunkiesSearchResult>();
		
		JSONObject data = (JSONObject) request("allseries.php?d=" + apikey);
		JSONArray list = (JSONArray) data.get("allseries");
		
		for (Object element : list) {
			JSONObject obj = (JSONObject) element;
			
			Integer sid = new Integer((String) obj.get("id"));
			String link = (String) obj.get("link");
			String mainTitle = (String) obj.get("short");
			String germanTitle = (String) obj.get("short_german");
			
			seriesList.add(new SerienjunkiesSearchResult(sid, link, mainTitle, germanTitle != null && germanTitle.length() > 0 ? germanTitle : null));
		}
		
		// populate cache
		cache.putSeriesList(seriesList);
		
		return seriesList;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, Locale locale) throws IOException {
		SerienjunkiesSearchResult series = (SerienjunkiesSearchResult) searchResult;
		
		// try cache first
		List<Episode> episodes = cache.getEpisodeList(series.getSeriesId());
		if (episodes != null)
			return episodes;
		
		// fetch episode data
		episodes = new ArrayList<Episode>(25);
		
		JSONObject data = (JSONObject) request("allepisodes.php?d=" + apikey + "&q=" + series.getSeriesId());
		JSONArray list = (JSONArray) data.get("allepisodes");
		
		for (int i = 0; i < list.size(); i++) {
			JSONObject obj = (JSONObject) list.get(i);
			
			Integer season = new Integer((String) obj.get("season"));
			Integer episode = new Integer((String) obj.get("episode"));
			String title = (String) obj.get("german");
			Date airdate = Date.parse((String) ((JSONObject) obj.get("airdates")).get("premiere"), "yyyy-MM-dd");
			
			episodes.add(new Episode(series.getName(), season, episode, title, i + 1, null, airdate));
		}
		
		// populate cache
		cache.putEpisodeList(episodes, series.getSeriesId());
		
		// make sure episodes are in ordered correctly
		sortEpisodes(episodes);
		
		return episodes;
	}
	

	protected Object request(String resource) throws IOException {
		URL url = new URL("https", host, resource);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		
		// disable SSL certificate validation
		connection.setSSLSocketFactory(createIgnoreCertificateSocketFactory());
		
		// fetch and parse json data
		Reader reader = getReader(connection);
		try {
			return JSONValue.parse(reader);
		} finally {
			reader.close();
		}
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return getEpisodeListLink(searchResult, "alle-serien-staffeln");
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return getEpisodeListLink(searchResult, "season" + season);
	}
	

	public URI getEpisodeListLink(SearchResult searchResult, String page) {
		return URI.create(String.format("http://www.serienjunkies.de/%s/%s.html", ((SerienjunkiesSearchResult) searchResult).getLink(), page));
	}
	

	public static class SerienjunkiesSearchResult extends SearchResult implements Serializable {
		
		protected int sid;
		protected String link;
		protected String mainTitle;
		protected String germanTitle;
		

		protected SerienjunkiesSearchResult() {
			// used by serializer
		}
		

		public SerienjunkiesSearchResult(int sid, String link, String mainTitle, String germanTitle) {
			this.sid = sid;
			this.link = link;
			this.mainTitle = mainTitle;
			this.germanTitle = germanTitle;
		}
		

		@Override
		public String getName() {
			return germanTitle != null ? germanTitle : mainTitle; // prefer german title
		}
		

		public int getSeriesId() {
			return sid;
		}
		

		public String getLink() {
			return link;
		}
		

		public String getMainTitle() {
			return mainTitle;
		}
		

		public String getGermanTitle() {
			return germanTitle;
		}
	}
	

	private static class SerienjunkiesCache {
		
		private final Cache cache;
		

		public SerienjunkiesCache(Cache cache) {
			this.cache = cache;
		}
		

		public void putSeriesList(Collection<SerienjunkiesSearchResult> anime) {
			cache.put(new Element(host + "SeriesList", anime.toArray(new SerienjunkiesSearchResult[0])));
		}
		

		public List<SerienjunkiesSearchResult> getSeriesList() {
			Element element = cache.get(host + "SeriesList");
			
			if (element != null)
				return Arrays.asList((SerienjunkiesSearchResult[]) element.getValue());
			
			return null;
		}
		

		public void putEpisodeList(Collection<Episode> episodes, int sid) {
			cache.put(new Element(host + "EpisodeList" + sid, episodes.toArray(new Episode[0])));
		}
		

		public List<Episode> getEpisodeList(int sid) {
			Element element = cache.get(host + "EpisodeList" + sid);
			
			if (element != null)
				return Arrays.asList((Episode[]) element.getValue());
			
			return null;
		}
		
	}
	
}
