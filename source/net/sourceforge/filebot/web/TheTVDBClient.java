
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getDocument;
import static net.sourceforge.tuned.XPathUtil.getTextContent;
import static net.sourceforge.tuned.XPathUtil.selectInteger;
import static net.sourceforge.tuned.XPathUtil.selectNodes;
import static net.sourceforge.tuned.XPathUtil.selectString;

import java.io.FileNotFoundException;
import java.io.IOException;
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
import javax.xml.parsers.ParserConfigurationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TheTVDBClient implements EpisodeListClient {
	
	private static final String host = "www.thetvdb.com";
	
	private final String apikey;
	
	private final Map<MirrorType, String> mirrors = new EnumMap<MirrorType, String>(MirrorType.class);
	
	private final TheTVDBCache cache = new TheTVDBCache(CacheManager.getInstance().getCache("web"));
	
	
	public TheTVDBClient(String apikey) {
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
	public List<SearchResult> search(String query) throws Exception {
		return search(query, Locale.ENGLISH);
	}
	

	public List<SearchResult> search(String query, Locale language) throws Exception {
		
		URL searchUrl = new URL("http", host, "/api/GetSeries.php?seriesname=" + URLEncoder.encode(query, "UTF-8") + "&language=" + language.getLanguage());
		
		Document dom = getDocument(searchUrl);
		
		List<Node> nodes = selectNodes("Data/Series", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			int seriesId = selectInteger("seriesid", node);
			String seriesName = selectString("SeriesName", node);
			
			searchResults.add(new TheTVDBSearchResult(seriesName, seriesId));
		}
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		return getEpisodeList((TheTVDBSearchResult) searchResult, Locale.ENGLISH);
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		
		List<Episode> episodes = new ArrayList<Episode>(25);
		
		// remember max. season, so we can throw a proper exception, in case an illegal season number was requested
		int maxSeason = 0;
		
		// filter given season from all seasons
		for (Episode episode : getEpisodeList(searchResult)) {
			try {
				int seasonNumber = Integer.parseInt(episode.getSeasonNumber());
				
				if (season == seasonNumber) {
					episodes.add(episode);
				}
				
				if (seasonNumber > maxSeason) {
					maxSeason = seasonNumber;
				}
			} catch (NumberFormatException e) {
				Logger.getLogger("global").log(Level.WARNING, "Illegal season number", e);
			}
		}
		
		if (episodes.isEmpty())
			throw new SeasonOutOfBoundsException(searchResult.getName(), season, maxSeason);
		
		return episodes;
	}
	

	public List<Episode> getEpisodeList(TheTVDBSearchResult searchResult, Locale language) throws Exception {
		
		List<Episode> episodes = cache.getEpisodeList(searchResult.getSeriesId(), language);
		
		if (episodes != null)
			return episodes;
		
		Document seriesRecord = getSeriesRecord(searchResult, language);
		
		// we could get the series name from the search result, but the language may not match the given parameter
		String seriesName = selectString("Data/Series/SeriesName", seriesRecord);
		
		List<Node> nodes = selectNodes("Data/Episode", seriesRecord);
		
		episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String episodeName = getTextContent("EpisodeName", node);
			String episodeNumber = getTextContent("EpisodeNumber", node);
			String seasonNumber = getTextContent("SeasonNumber", node);
			
			episodes.add(new Episode(seriesName, seasonNumber, episodeNumber, episodeName));
			
			if (episodeNumber.equals("1")) {
				// cache seasonId for each season (always when we are at the first episode)
				// because it might be required by getEpisodeListLink
				cache.putSeasonId(searchResult.getSeriesId(), Integer.parseInt(seasonNumber), Integer.parseInt(getTextContent("seasonid", node)));
			}
		}
		
		cache.putEpisodeList(searchResult.getSeriesId(), language, episodes);
		return episodes;
	}
	

	public Document getSeriesRecord(TheTVDBSearchResult searchResult, Locale language) throws IOException, SAXException, ParserConfigurationException {
		
		URL seriesRecordUrl = new URL(getMirror(MirrorType.ZIP) + "/api/" + apikey + "/series/" + searchResult.getSeriesId() + "/all/" + language.getLanguage() + ".zip");
		
		ZipInputStream zipInputStream = new ZipInputStream(seriesRecordUrl.openStream());
		ZipEntry zipEntry;
		
		try {
			String seriesRecordName = language.getLanguage() + ".xml";
			
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				if (seriesRecordName.equals(zipEntry.getName())) {
					return getDocument(zipInputStream);
				}
			}
			
			// zip file must contain the series record
			throw new FileNotFoundException(String.format("Archive must contain %s: %s", seriesRecordName, seriesRecordUrl));
		} finally {
			zipInputStream.close();
		}
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		int seriesId = ((TheTVDBSearchResult) searchResult).getSeriesId();
		
		return URI.create("http://www.thetvdb.com/?tab=seasonall&id=" + seriesId);
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		int seriesId = ((TheTVDBSearchResult) searchResult).getSeriesId();
		
		try {
			Integer seasonId = cache.getSeasonId(seriesId, season);
			
			if (seasonId == null) {
				// get episode xml from first episode of given season
				Document dom = getDocument(new URL("http", host, "/api/" + apikey + "/series/" + seriesId + "/default/" + season + "/1/en.xml"));
				
				seasonId = selectInteger("Data/Episode/seasonid", dom);
				
				cache.putSeasonId(seriesId, season, seasonId);
			}
			
			return new URI("http://www.thetvdb.com/?tab=season&seriesid=" + seriesId + "&seasonid=" + seasonId);
		} catch (Exception e) {
			Logger.getLogger("global").log(Level.WARNING, "Failed to retrieve season id", e);
		}
		
		return null;
	}
	

	protected String getMirror(MirrorType mirrorType) throws IOException, SAXException, ParserConfigurationException {
		synchronized (mirrors) {
			if (mirrors.isEmpty()) {
				// initialize mirrors
				URL mirrorUrl = new URL("http", host, "/api/" + apikey + "/mirrors.xml");
				
				Document dom = getDocument(mirrorUrl);
				
				// all mirrors by type
				Map<MirrorType, List<String>> mirrorListMap = new EnumMap<MirrorType, List<String>>(MirrorType.class);
				
				// initialize mirror list per type
				for (MirrorType type : MirrorType.values()) {
					mirrorListMap.put(type, new ArrayList<String>(5));
				}
				
				// traverse all mirrors
				for (Node node : selectNodes("Mirrors/Mirror", dom)) {
					// mirror data
					String mirror = selectString("mirrorpath", node);
					int typeMask = selectInteger("typemask", node);
					
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
		

		public void putSeasonId(int seriesId, int seasonNumber, int seasonId) {
			cache.put(new Element(key(host, seriesId, seasonNumber, "SeasonId"), seasonId));
		}
		

		public Integer getSeasonId(int seriesId, int seasonNumber) {
			Element element = cache.get(key(host, seriesId, seasonNumber, "SeasonId"));
			
			if (element != null)
				return (Integer) element.getValue();
			
			return null;
		}
		

		public void putEpisodeList(int seriesId, Locale language, List<Episode> episodes) {
			cache.put(new Element(key(host, seriesId, language, "EpisodeList"), episodes));
		}
		

		@SuppressWarnings("unchecked")
		public List<Episode> getEpisodeList(int seriesId, Locale language) {
			Element element = cache.get(key(host, seriesId, language.getLanguage(), "EpisodeList"));
			
			if (element != null)
				return (List<Episode>) element.getValue();
			
			return null;
		}
		

		private String key(Object... key) {
			return Arrays.toString(key);
		}
		
	}
	
}
