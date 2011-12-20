
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
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

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TheTVDBClient.BannerDescriptor.BannerProperty;
import net.sourceforge.tuned.FileUtilities;


public class TheTVDBClient extends AbstractEpisodeListProvider {
	
	private final String host = "www.thetvdb.com";
	
	private final Map<MirrorType, String> mirrors = new EnumMap<MirrorType, String>(MirrorType.class);
	
	private final String apikey;
	
	
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
	public ResultCache getCache() {
		return new ResultCache(host, CacheManager.getInstance().getCache("web-datasource"));
	}
	
	
	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale language) throws Exception {
		// perform online search
		URL url = getResource(null, "/api/GetSeries.php?seriesname=" + encode(query) + "&language=" + language.getLanguage());
		Document dom = getDocument(url);
		
		List<Node> nodes = selectNodes("Data/Series", dom);
		Map<Integer, TheTVDBSearchResult> resultSet = new LinkedHashMap<Integer, TheTVDBSearchResult>();
		
		for (Node node : nodes) {
			int sid = getIntegerContent("seriesid", node);
			String seriesName = getTextContent("SeriesName", node);
			
			if (!resultSet.containsKey(sid)) {
				resultSet.put(sid, new TheTVDBSearchResult(seriesName, sid));
			}
		}
		
		return new ArrayList<SearchResult>(resultSet.values());
	}
	
	
	@Override
	public List<Episode> fetchEpisodeList(SearchResult searchResult, Locale language) throws Exception {
		TheTVDBSearchResult series = (TheTVDBSearchResult) searchResult;
		
		Document seriesRecord = getSeriesRecord(series, language);
		
		// we could get the series name from the search result, but the language may not match the given parameter
		String seriesName = selectString("Data/Series/SeriesName", seriesRecord);
		Date seriesStartDate = Date.parse(selectString("Data/Series/FirstAired", seriesRecord), "yyyy-MM-dd");
		
		List<Node> nodes = selectNodes("Data/Episode", seriesRecord);
		
		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
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
				
				// use given episode number as special number or count specials by ourselves
				Integer specialNumber = (episodeNumber != null) ? episodeNumber : filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesName, seriesStartDate, seasonNumber, null, episodeName, null, specialNumber, airdate));
			} else {
				// handle as normal episode
				episodes.add(new Episode(seriesName, seriesStartDate, seasonNumber, episodeNumber, episodeName, absoluteNumber, null, airdate));
			}
		}
		
		// episodes my not be ordered by DVD episode number
		sortEpisodes(episodes);
		
		// add specials at the end
		episodes.addAll(specials);
		
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
	
	
	public TheTVDBSearchResult lookupByID(int id, Locale language) throws Exception {
		try {
			URL baseRecordLocation = getResource(MirrorType.XML, "/api/" + apikey + "/series/" + id + "/all/" + language.getLanguage() + ".xml");
			Document baseRecord = getDocument(baseRecordLocation);
			
			String name = selectString("//SeriesName", baseRecord);
			return new TheTVDBSearchResult(name, id);
		} catch (FileNotFoundException e) {
			// illegal series id
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to retrieve base series record: " + e.getMessage());
			return null;
		}
	}
	
	
	public TheTVDBSearchResult lookupByIMDbID(int imdbid, Locale language) throws Exception {
		URL query = getResource(null, "/api/GetSeriesByRemoteID.php?imdbid=" + imdbid + "&language=" + language.getLanguage());
		Document dom = getDocument(query);
		
		String id = selectString("//seriesid", dom);
		String name = selectString("//SeriesName", dom);
		
		if (id == null || id.isEmpty() || name == null || name.isEmpty())
			return null;
		
		return new TheTVDBSearchResult(name, Integer.parseInt(id));
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
			// get episode xml from first episode of given season
			Document dom = getDocument(getResource(MirrorType.XML, "/api/" + apikey + "/series/" + seriesId + "/default/" + season + "/1/en.xml"));
			int seasonId = Integer.valueOf(selectString("Data/Episode/seasonid", dom));
			
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
		
		protected int seriesId;
		
		
		protected TheTVDBSearchResult() {
			// used by serializer
		}
		
		
		public TheTVDBSearchResult(String seriesName, int seriesId) {
			super(seriesName);
			this.seriesId = seriesId;
		}
		
		
		public int getSeriesId() {
			return seriesId;
		}
		
		
		@Override
		public int hashCode() {
			return seriesId;
		}
		
		
		@Override
		public boolean equals(Object object) {
			if (object instanceof TheTVDBSearchResult) {
				TheTVDBSearchResult other = (TheTVDBSearchResult) object;
				return this.seriesId == other.seriesId;
			}
			
			return false;
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
	
	
	/**
	 * Search for a series banner matching the given parameters
	 * 
	 * @see http://thetvdb.com/wiki/index.php/API:banners.xml
	 */
	public BannerDescriptor getBanner(TheTVDBSearchResult series, String bannerType, String bannerType2, Integer season, Locale locale) throws Exception {
		// search for a banner matching the selector
		for (BannerDescriptor it : getBannerList(series.seriesId)) {
			if ((bannerType == null || it.getBannerType().equalsIgnoreCase(bannerType)) && (bannerType2 == null || it.getBannerType2().equalsIgnoreCase(bannerType2)) && (season == null || it.getSeason().equals(season))
					&& ((locale == null && it.getLocale().getLanguage().equals("en")) || it.getLocale().getLanguage().equals(locale.getLanguage()))) {
				return it;
			}
		}
		
		return null;
	}
	
	
	public List<BannerDescriptor> getBannerList(int seriesid) throws Exception {
		Document dom = getDocument(getResource(MirrorType.XML, "/api/" + apikey + "/series/" + seriesid + "/banners.xml"));
		
		List<Node> nodes = selectNodes("//Banner", dom);
		List<BannerDescriptor> banners = new ArrayList<BannerDescriptor>();
		
		for (Node node : nodes) {
			try {
				EnumMap<BannerProperty, String> item = new EnumMap<BannerProperty, String>(BannerProperty.class);
				
				// insert banner mirror
				item.put(BannerProperty.BannerMirror, getResource(MirrorType.BANNER, "/banners/").toString());
				
				// copy values from xml
				for (BannerProperty key : BannerProperty.values()) {
					String value = getTextContent(key.name(), node);
					if (value != null && value.length() > 0) {
						item.put(key, getTextContent(key.name(), node));
					}
				}
				
				banners.add(new BannerDescriptor(item));
			} catch (Exception e) {
				// log and ignore
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid banner descriptor", e);
			}
		}
		
		return banners;
	}
	
	
	public static class BannerDescriptor {
		
		public static enum BannerProperty {
			id,
			BannerMirror,
			BannerPath,
			BannerType,
			BannerType2,
			Season,
			Colors,
			Language,
			Rating,
			RatingCount,
			SeriesName,
			ThumbnailPath,
			VignettePath
		}
		
		
		private EnumMap<BannerProperty, String> fields;
		
		
		protected BannerDescriptor(Map<BannerProperty, String> fields) {
			this.fields = new EnumMap<BannerProperty, String>(fields);
		}
		
		
		public URL getMirrorUrl() throws MalformedURLException {
			return new URL(fields.get(BannerProperty.BannerMirror));
		}
		
		
		public URL getUrl() throws MalformedURLException {
			return new URL(getMirrorUrl(), fields.get(BannerProperty.BannerPath));
		}
		
		
		public String getExtension() {
			return FileUtilities.getExtension(fields.get(BannerProperty.BannerPath));
		}
		
		
		public int getId() {
			return Integer.parseInt(fields.get(BannerProperty.id));
		}
		
		
		public String getBannerType() {
			return fields.get(BannerProperty.BannerType);
		}
		
		
		public String getBannerType2() {
			return fields.get(BannerProperty.BannerType2);
		}
		
		
		public Integer getSeason() {
			try {
				return new Integer(fields.get(BannerProperty.Season));
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
		
		public String getColors() {
			return fields.get(BannerProperty.Colors);
		}
		
		
		public Locale getLocale() {
			return new Locale(fields.get(BannerProperty.Language));
		}
		
		
		public double getRating() {
			return Double.parseDouble(fields.get(BannerProperty.Rating));
		}
		
		
		public int getRatingCount() {
			return Integer.parseInt(fields.get(BannerProperty.RatingCount));
		}
		
		
		public boolean hasSeriesName() {
			return Boolean.parseBoolean(fields.get(BannerProperty.SeriesName));
		}
		
		
		public URL getThumbnailUrl() throws MalformedURLException {
			return new URL(getMirrorUrl(), fields.get(BannerProperty.ThumbnailPath));
		}
		
		
		public URL getVignetteUrl() throws MalformedURLException {
			return new URL(getMirrorUrl(), fields.get(BannerProperty.VignettePath));
		}
		
		
		@Override
		public String toString() {
			return fields.toString();
		}
	}
	
}
