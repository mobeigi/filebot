
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeListUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;


public class TheTVDBClient extends AbstractEpisodeListProvider {
	
	private static final String host = "www.thetvdb.com";
	
	private final String apikey;
	
	private final Map<MirrorType, String> mirrors = new EnumMap<MirrorType, String>(MirrorType.class);
	
	private final TheTVDBCache cache = new TheTVDBCache(CacheManager.getInstance().getCache("web"));
	

	public TheTVDBClient(String apikey) {
		if (apikey == null)
			throw new NullPointerException("apikey must not be null");
		
		this.apikey = apikey;
	}
	

	@Override
	public String getName() {
		return "TheTVDB";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.thetvdb");
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public boolean hasLocaleSupport() {
		return true;
	}
	

	@Override
	public List<SearchResult> search(String query, Locale language) throws Exception {
		// check if the exact series name is already cached
		Integer cachedResult = cache.getSeriesId(query, language);
		
		if (cachedResult != null)
			return Arrays.asList(new SearchResult[] { new TheTVDBSearchResult(query, cachedResult) });
		
		// perform online search
		URL url = getResource(null, "/api/GetSeries.php?seriesname=" + URLEncoder.encode(query, "UTF-8") + "&language=" + language.getLanguage());
		Document dom = getDocument(url);
		
		List<Node> nodes = selectNodes("Data/Series", dom);
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			int seriesId = getIntegerContent("seriesid", node);
			String seriesName = getTextContent("SeriesName", node);
			
			searchResults.add(new TheTVDBSearchResult(seriesName, seriesId));
			cache.putSeriesId(seriesName, language, seriesId);
		}
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, Locale language) throws Exception {
		TheTVDBSearchResult series = (TheTVDBSearchResult) searchResult;
		List<Episode> episodes = cache.getEpisodeList(series.getSeriesId(), language);
		
		if (episodes != null)
			return episodes;
		
		Document seriesRecord = getSeriesRecord(series, language);
		
		// we could get the series name from the search result, but the language may not match the given parameter
		String seriesName = selectString("Data/Series/SeriesName", seriesRecord);
		
		List<Node> nodes = selectNodes("Data/Episode", seriesRecord);
		
		episodes = new ArrayList<Episode>(nodes.size());
		List<Episode> specials = new ArrayList<Episode>(5);
		
		for (Node node : nodes) {
			String episodeName = getTextContent("EpisodeName", node);
			String dvdSeasonNumber = getTextContent("DVD_season", node);
			String dvdEpisodeNumber = getTextContent("DVD_episodenumber", node);
			Integer absoluteNumber = getIntegerContent("absolute_number", node);
			Date airdate = Date.parse(getTextContent("FirstAired", node), "yyyy-MM-dd");
			
			// prefer DVD SxE numbers if available
			Integer seasonNumber;
			Integer episodeNumber;
			
			try {
				seasonNumber = new Integer(dvdSeasonNumber);
				episodeNumber = new Float(dvdEpisodeNumber).intValue();
			} catch (Exception e) {
				seasonNumber = getIntegerContent("SeasonNumber", node);
				episodeNumber = getIntegerContent("EpisodeNumber", node);
			}
			
			if (seasonNumber == null || seasonNumber == 0) {
				// handle as special episode
				Integer airsBefore = getIntegerContent("airsbefore_season", node);
				if (airsBefore != null) {
					seasonNumber = airsBefore;
				}
				
				Integer specialNumber = filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesName, seasonNumber, null, episodeName, null, specialNumber, airdate));
			} else {
				// handle as normal episode
				episodes.add(new Episode(seriesName, seasonNumber, episodeNumber, episodeName, absoluteNumber, null, airdate));
			}
			
			if (episodeNumber == 1) {
				try {
					// cache seasonId for each season (always when we are at the first episode)
					// because it might be required by getEpisodeListLink
					cache.putSeasonId(series.getSeriesId(), seasonNumber, getIntegerContent("seasonid", node));
				} catch (NumberFormatException e) {
					// season/episode is not a number, just ignore
				}
			}
		}
		
		// episodes my not be ordered by DVD episode number
		sortEpisodes(episodes);
		
		// add specials at the end
		episodes.addAll(specials);
		
		cache.putEpisodeList(series.getSeriesId(), language, episodes);
		return episodes;
	}
	

	public Document getSeriesRecord(TheTVDBSearchResult searchResult, Locale language) throws Exception {
		URL seriesRecord = getResource(MirrorType.ZIP, "/api/" + apikey + "/series/" + searchResult.getSeriesId() + "/all/" + language.getLanguage() + ".zip");
		
		ZipInputStream zipInputStream = new ZipInputStream(seriesRecord.openStream());
		ZipEntry zipEntry;
		
		try {
			String seriesRecordName = language.getLanguage() + ".xml";
			
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				if (seriesRecordName.equals(zipEntry.getName())) {
					return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zipInputStream);
				}
			}
			
			// zip file must contain the series record
			throw new FileNotFoundException(String.format("Archive must contain %s: %s", seriesRecordName, seriesRecord));
		} finally {
			zipInputStream.close();
		}
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		int seriesId = ((TheTVDBSearchResult) searchResult).getSeriesId();
		
		return URI.create("http://" + host + "/?tab=seasonall&id=" + seriesId);
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		int seriesId = ((TheTVDBSearchResult) searchResult).getSeriesId();
		
		try {
			Integer seasonId = cache.getSeasonId(seriesId, season);
			
			if (seasonId == null) {
				// get episode xml from first episode of given season
				URL url = getResource(MirrorType.XML, "/api/" + apikey + "/series/" + seriesId + "/default/" + season + "/1/en.xml");
				Document dom = getDocument(url);
				
				seasonId = Integer.valueOf(selectString("Data/Episode/seasonid", dom));
				cache.putSeasonId(seriesId, season, seasonId);
			}
			
			return new URI("http://" + host + "/?tab=season&seriesid=" + seriesId + "&seasonid=" + seasonId);
		} catch (Exception e) {
			// log and ignore any IOException
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to retrieve season id", e);
		}
		
		return null;
	}
	

	protected String getMirror(MirrorType mirrorType) throws Exception {
		synchronized (mirrors) {
			if (mirrors.isEmpty()) {
				// initialize mirrors
				Document dom = getDocument(getResource(null, "/api/" + apikey + "/mirrors.xml"));
				
				// all mirrors by type
				Map<MirrorType, List<String>> mirrorListMap = new EnumMap<MirrorType, List<String>>(MirrorType.class);
				
				// initialize mirror list per type
				for (MirrorType type : MirrorType.values()) {
					mirrorListMap.put(type, new ArrayList<String>(5));
				}
				
				// traverse all mirrors
				for (Node node : selectNodes("Mirrors/Mirror", dom)) {
					// mirror data
					String mirror = getTextContent("mirrorpath", node);
					int typeMask = Integer.parseInt(getTextContent("typemask", node));
					
					// add mirror to the according type lists
					for (MirrorType type : MirrorType.fromTypeMask(typeMask)) {
						mirrorListMap.get(type).add(mirror);
					}
				}
				
				// put random entry from each type list into mirrors
				Random random = new Random();
				
				for (MirrorType type : MirrorType.values()) {
					List<String> list = mirrorListMap.get(type);
					
					if (!list.isEmpty()) {
						mirrors.put(type, list.get(random.nextInt(list.size())));
					}
				}
			}
			
			return mirrors.get(mirrorType);
		}
	}
	

	protected URL getResource(MirrorType mirrorType, String path) throws Exception {
		// use default server
		if (mirrorType == null)
			return new URL("http", host, path);
		
		// use mirror
		return new URL(getMirror(mirrorType) + path);
	}
	

	public static class TheTVDBSearchResult extends SearchResult {
		
		private final int seriesId;
		

		public TheTVDBSearchResult(String seriesName, int seriesId) {
			super(seriesName);
			this.seriesId = seriesId;
		}
		

		public int getSeriesId() {
			return seriesId;
		}
		
	}
	

	protected static enum MirrorType {
		XML(1),
		BANNER(2),
		ZIP(4);
		
		private final int bitMask;
		

		private MirrorType(int bitMask) {
			this.bitMask = bitMask;
		}
		

		public static EnumSet<MirrorType> fromTypeMask(int typeMask) {
			// initialize enum set with all types
			EnumSet<MirrorType> enumSet = EnumSet.allOf(MirrorType.class);
			
			for (MirrorType type : values()) {
				if ((typeMask & type.bitMask) == 0) {
					// remove types that are not set
					enumSet.remove(type);
				}
			}
			
			return enumSet;
		};
		
	}
	

	private static class TheTVDBCache {
		
		private final Cache cache;
		

		public TheTVDBCache(Cache cache) {
			this.cache = cache;
		}
		

		public void putSeriesId(String seriesName, Locale language, int seriesId) {
			cache.put(new Element(key(host, "SeriesId", seriesName, language.getLanguage()), seriesId));
		}
		

		public Integer getSeriesId(String seriesName, Locale language) {
			Element element = cache.get(key(host, "SeriesId", seriesName, language.getLanguage()));
			
			if (element != null)
				return (Integer) element.getValue();
			
			return null;
		}
		

		public void putSeasonId(int seriesId, int seasonNumber, int seasonId) {
			cache.put(new Element(key(host, "SeasonId", seriesId, seasonNumber), seasonId));
		}
		

		public Integer getSeasonId(int seriesId, int seasonNumber) {
			Element element = cache.get(key(host, "SeasonId", seriesId, seasonNumber));
			
			if (element != null)
				return (Integer) element.getValue();
			
			return null;
		}
		

		public void putEpisodeList(int seriesId, Locale language, List<Episode> episodes) {
			cache.put(new Element(key(host, "EpisodeList", seriesId, language.getLanguage()), episodes));
		}
		

		@SuppressWarnings("unchecked")
		public List<Episode> getEpisodeList(int seriesId, Locale language) {
			Element element = cache.get(key(host, "EpisodeList", seriesId, language.getLanguage()));
			
			if (element != null)
				return (List<Episode>) element.getValue();
			
			return null;
		}
		

		private String key(Object... key) {
			return Arrays.toString(key);
		}
		
	}
	
}
