package net.sourceforge.filebot.web;

import static java.util.Arrays.*;
import static net.sourceforge.filebot.web.EpisodeUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
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
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TheTVDBClient.BannerDescriptor.BannerProperty;
import net.sourceforge.filebot.web.TheTVDBClient.SeriesInfo.SeriesProperty;
import net.sourceforge.tuned.FileUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

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

	public String getLanguageCode(Locale locale) {
		String code = locale.getLanguage();

		// Java language code => TheTVDB language code
		if (code.equals("iw")) // Hebrew
			return "he";
		if (code.equals("hi")) // Hungarian
			return "hu";
		if (code.equals("in")) // Indonesian
			return "id";
		if (code.equals("ro")) // Russian
			return "ru";

		return code;
	}

	@Override
	public ResultCache getCache() {
		return new ResultCache(host, Cache.getCache("web-datasource"));
	}

	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception {
		// perform online search
		Document dom = getXmlResource(MirrorType.SEARCH, "/api/GetSeries.php?seriesname=" + encode(query, true) + "&language=" + getLanguageCode(locale));

		List<Node> nodes = selectNodes("Data/Series", dom);
		Map<Integer, TheTVDBSearchResult> resultSet = new LinkedHashMap<Integer, TheTVDBSearchResult>();

		for (Node node : nodes) {
			int sid = getIntegerContent("seriesid", node);
			String seriesName = getTextContent("SeriesName", node);

			List<String> aliasNames = new ArrayList<String>(2);
			for (Node aliasNode : selectNodes("AliasNames", node)) {
				aliasNames.add(getTextContent(aliasNode));
			}

			if (!resultSet.containsKey(sid)) {
				resultSet.put(sid, new TheTVDBSearchResult(seriesName, aliasNames.toArray(new String[0]), sid));
			}
		}

		return new ArrayList<SearchResult>(resultSet.values());
	}

	@Override
	public List<Episode> fetchEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
		TheTVDBSearchResult series = (TheTVDBSearchResult) searchResult;
		Document dom = getXmlResource(MirrorType.XML, "/api/" + apikey + "/series/" + series.getSeriesId() + "/all/" + locale.getLanguage() + ".xml");

		// we could get the series name from the search result, but the language may not match the given parameter
		String seriesName = selectString("Data/Series/SeriesName", dom);
		Date seriesStartDate = Date.parse(selectString("Data/Series/FirstAired", dom), "yyyy-MM-dd");

		List<Node> nodes = selectNodes("Data/Episode", dom);

		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		List<Episode> specials = new ArrayList<Episode>(5);

		for (Node node : nodes) {
			String episodeName = getTextContent("EpisodeName", node);
			String dvdSeasonNumber = getTextContent("DVD_season", node);
			String dvdEpisodeNumber = getTextContent("DVD_episodenumber", node);
			Integer absoluteNumber = getIntegerContent("absolute_number", node);
			Date airdate = Date.parse(getTextContent("FirstAired", node), "yyyy-MM-dd");

			// default numbering
			Integer episodeNumber = getIntegerContent("EpisodeNumber", node);
			Integer seasonNumber = getIntegerContent("SeasonNumber", node);

			if (seasonNumber == null || seasonNumber == 0) {
				// handle as special episode
				Integer airsBefore = getIntegerContent("airsbefore_season", node);
				if (airsBefore != null) {
					seasonNumber = airsBefore;
				}

				// use given episode number as special number or count specials by ourselves
				Integer specialNumber = (episodeNumber != null) ? episodeNumber : filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesName, seriesStartDate, seasonNumber, null, episodeName, null, specialNumber, airdate, searchResult));
			} else {
				// handle as normal episode
				if (sortOrder == SortOrder.Absolute) {
					if (absoluteNumber != null) {
						episodeNumber = absoluteNumber;
						seasonNumber = null;
					}
				} else if (sortOrder == SortOrder.DVD) {
					try {
						episodeNumber = new Float(dvdEpisodeNumber).intValue();
						seasonNumber = new Integer(dvdSeasonNumber);
					} catch (Exception e) {
						// ignore, fallback to default numbering
					}
				}

				episodes.add(new Episode(seriesName, seriesStartDate, seasonNumber, episodeNumber, episodeName, absoluteNumber, null, airdate, searchResult));
			}
		}

		// episodes my not be ordered by DVD episode number
		sortEpisodes(episodes);

		// add specials at the end
		episodes.addAll(specials);

		return episodes;
	}

	public TheTVDBSearchResult lookupByID(int id, Locale locale) throws Exception {
		TheTVDBSearchResult cachedItem = getCache().getData("lookupByID", id, locale, TheTVDBSearchResult.class);
		if (cachedItem != null) {
			return cachedItem;
		}

		Document dom = getXmlResource(MirrorType.XML, "/api/" + apikey + "/series/" + id + "/all/" + getLanguageCode(locale) + ".xml");
		String name = selectString("//SeriesName", dom);

		TheTVDBSearchResult series = new TheTVDBSearchResult(name, id);
		getCache().putData("lookupByID", id, locale, series);
		return series;
	}

	public TheTVDBSearchResult lookupByIMDbID(int imdbid, Locale locale) throws Exception {
		TheTVDBSearchResult cachedItem = getCache().getData("lookupByIMDbID", imdbid, locale, TheTVDBSearchResult.class);
		if (cachedItem != null) {
			return cachedItem;
		}

		Document dom = getXmlResource(null, "/api/GetSeriesByRemoteID.php?imdbid=" + imdbid + "&language=" + getLanguageCode(locale));

		String id = selectString("//seriesid", dom);
		String name = selectString("//SeriesName", dom);

		if (id == null || id.isEmpty() || name == null || name.isEmpty())
			return null;

		TheTVDBSearchResult series = new TheTVDBSearchResult(name, Integer.parseInt(id));
		getCache().putData("lookupByIMDbID", imdbid, locale, series);
		return series;
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create("http://" + host + "/?tab=seasonall&id=" + ((TheTVDBSearchResult) searchResult).getSeriesId());
	}

	protected String getMirror(MirrorType mirrorType) throws IOException {
		synchronized (mirrors) {
			if (mirrors.isEmpty()) {
				// try cache first
				try {
					@SuppressWarnings("unchecked")
					Map<MirrorType, String> cachedMirrors = getCache().getData("mirrors", null, null, Map.class);
					if (cachedMirrors != null) {
						mirrors.putAll(cachedMirrors);
						return mirrors.get(mirrorType);
					}
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
				}

				// initialize mirrors
				Document dom = getXmlResource(null, "/api/" + apikey + "/mirrors.xml");

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

				getCache().putData("mirrors", null, null, mirrors);
			}

			return mirrors.get(mirrorType);
		}
	}

	protected Document getXmlResource(final MirrorType mirrorType, final String path) throws IOException {
		CachedXmlResource resource = new CachedXmlResource(path) {

			protected URL getResourceLocation(String path) throws IOException {
				return getResourceURL(mirrorType, path);
			};
		};

		// fetch data or retrieve from cache
		try {
			return resource.getDocument();
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Resource not found: " + getResourceURL(mirrorType, path)); // simplify error message
		}
	}

	protected URL getResourceURL(MirrorType mirrorType, String path) throws IOException {
		if (mirrorType != null) {
			// use mirror
			String mirror = getMirror(mirrorType);
			if (mirror != null && mirror.length() > 0) {
				return new URL(mirror + path);
			}
		}

		// use default server
		return new URL("http", host, path);
	}

	protected static enum MirrorType {
		XML(1), BANNER(2), ZIP(4), SEARCH(1);

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

	public SeriesInfo getSeriesInfoByID(int thetvdbid, Locale locale) throws Exception {
		return getSeriesInfo(new TheTVDBSearchResult(null, thetvdbid), locale);
	}

	public SeriesInfo getSeriesInfoByIMDbID(int imdbid, Locale locale) throws Exception {
		return getSeriesInfo(lookupByIMDbID(imdbid, locale), locale);
	}

	public SeriesInfo getSeriesInfoByName(String name, Locale locale) throws Exception {
		for (SearchResult it : search(name, locale)) {
			if (name.equalsIgnoreCase(it.getName())) {
				return getSeriesInfo((TheTVDBSearchResult) it, locale);
			}
		}

		return null;
	}

	public SeriesInfo getSeriesInfo(TheTVDBSearchResult searchResult, Locale locale) throws Exception {
		// check cache first
		SeriesInfo cachedItem = getCache().getData("seriesInfo", searchResult.seriesId, locale, SeriesInfo.class);
		if (cachedItem != null) {
			return cachedItem;
		}

		Document dom = getXmlResource(MirrorType.XML, "/api/" + apikey + "/series/" + searchResult.seriesId + "/" + getLanguageCode(locale) + ".xml");

		Node node = selectNode("//Series", dom);
		Map<SeriesProperty, String> fields = new EnumMap<SeriesProperty, String>(SeriesProperty.class);

		// remember banner mirror
		fields.put(SeriesProperty.BannerMirror, getResourceURL(MirrorType.BANNER, "/banners/").toString());

		// copy values from xml
		for (SeriesProperty key : SeriesProperty.values()) {
			String value = getTextContent(key.name(), node);
			if (value != null && value.length() > 0) {
				fields.put(key, value);
			}
		}

		SeriesInfo seriesInfo = new SeriesInfo(fields);
		getCache().putData("seriesInfo", searchResult.seriesId, locale, seriesInfo);
		return seriesInfo;
	}

	public static class SeriesInfo implements Serializable {

		public static enum SeriesProperty {
			id, Actors, Airs_DayOfWeek, Airs_Time, ContentRating, FirstAired, Genre, IMDB_ID, Language, Network, Overview, Rating, RatingCount, Runtime, SeriesName, Status, BannerMirror, banner, fanart, poster
		}

		protected Map<SeriesProperty, String> fields;

		protected SeriesInfo() {
			// used by serializer
		}

		protected SeriesInfo(Map<SeriesProperty, String> fields) {
			this.fields = new EnumMap<SeriesProperty, String>(fields);
		}

		public String get(Object key) {
			return fields.get(SeriesProperty.valueOf(key.toString()));
		}

		public String get(SeriesProperty key) {
			return fields.get(key);
		}

		public Integer getId() {
			// e.g. 80348
			try {
				return Integer.parseInt(get(SeriesProperty.id));
			} catch (Exception e) {
				return null;
			}
		}

		public List<String> getActors() {
			// e.g. |Zachary Levi|Adam Baldwin|Yvonne Strzechowski|
			return split(get(SeriesProperty.Actors));
		}

		public List<String> getGenres() {
			// e.g. |Comedy|
			return split(get(SeriesProperty.Genre));
		}

		protected List<String> split(String values) {
			List<String> items = new ArrayList<String>();
			if (values != null && values.length() > 0) {
				for (String it : values.split("[|]")) {
					it = it.trim();
					if (it.length() > 0) {
						items.add(it);
					}
				}
			}
			return items;
		}

		public String getAirDayOfWeek() {
			// e.g. Monday
			return get(SeriesProperty.Airs_DayOfWeek);
		}

		public String getAirTime() {
			// e.g. 8:00 PM
			return get(SeriesProperty.Airs_Time);
		}

		public Date getFirstAired() {
			// e.g. 2007-09-24
			return Date.parse(get(SeriesProperty.FirstAired), "yyyy-MM-dd");
		}

		public String getContentRating() {
			// e.g. TV-PG
			return get(SeriesProperty.ContentRating);
		}

		public String getCertification() {
			return getContentRating(); // another getter for compability reasons
		}

		public Integer getImdbId() {
			// e.g. tt0934814
			try {
				return Integer.parseInt(get(SeriesProperty.IMDB_ID).substring(2));
			} catch (Exception e) {
				return null;
			}
		}

		public Locale getLanguage() {
			// e.g. en
			try {
				return new Locale(get(SeriesProperty.Language));
			} catch (Exception e) {
				return null;
			}
		}

		public String getOverview() {
			// e.g. Zachary Levi (Less Than Perfect) plays Chuck...
			return get(SeriesProperty.Overview);
		}

		public Double getRating() {
			// e.g. 9.0
			try {
				return Double.parseDouble(get(SeriesProperty.Rating));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getRatingCount() {
			// e.g. 696
			try {
				return Integer.parseInt(get(SeriesProperty.RatingCount));
			} catch (Exception e) {
				return null;
			}
		}

		public String getRuntime() {
			// e.g. 30
			return get(SeriesProperty.Runtime);
		}

		public String getName() {
			// e.g. Chuck
			return get(SeriesProperty.SeriesName);
		}

		public String getNetwork() {
			// e.g. CBS
			return get(SeriesProperty.Network);
		}

		public String getStatus() {
			// e.g. Continuing
			return get(SeriesProperty.Status);
		}

		public URL getBannerMirrorUrl() {
			try {
				return new URL(get(BannerProperty.BannerMirror));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getBannerUrl() throws MalformedURLException {
			try {
				return new URL(getBannerMirrorUrl(), get(SeriesProperty.banner));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getFanartUrl() {
			try {
				return new URL(getBannerMirrorUrl(), get(SeriesProperty.fanart));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getPosterUrl() {
			try {
				return new URL(getBannerMirrorUrl(), get(SeriesProperty.poster));
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return fields.toString();
		}
	}

	/**
	 * Search for a series banner matching the given parameters
	 * 
	 * @see http://thetvdb.com/wiki/index.php/API:banners.xml
	 */
	public BannerDescriptor getBanner(TheTVDBSearchResult series, Map<?, ?> filterDescriptor) throws Exception {
		EnumMap<BannerProperty, String> filter = new EnumMap<BannerProperty, String>(BannerProperty.class);
		for (Entry<?, ?> it : filterDescriptor.entrySet()) {
			if (it.getValue() != null) {
				filter.put(BannerProperty.valueOf(it.getKey().toString()), it.getValue().toString());
			}
		}

		// search for a banner matching the selector
		for (BannerDescriptor it : getBannerList(series)) {
			if (it.fields.entrySet().containsAll(filter.entrySet())) {
				return it;
			}
		}

		return null;
	}

	public List<BannerDescriptor> getBannerList(TheTVDBSearchResult series) throws Exception {
		// check cache first
		BannerDescriptor[] cachedList = getCache().getData("banners", series.seriesId, null, BannerDescriptor[].class);
		if (cachedList != null) {
			return asList(cachedList);
		}

		Document dom = getXmlResource(MirrorType.XML, "/api/" + apikey + "/series/" + series.seriesId + "/banners.xml");

		List<Node> nodes = selectNodes("//Banner", dom);
		List<BannerDescriptor> banners = new ArrayList<BannerDescriptor>();

		for (Node node : nodes) {
			try {
				Map<BannerProperty, String> item = new EnumMap<BannerProperty, String>(BannerProperty.class);

				// insert banner mirror
				item.put(BannerProperty.BannerMirror, getResourceURL(MirrorType.BANNER, "/banners/").toString());

				// copy values from xml
				for (BannerProperty key : BannerProperty.values()) {
					String value = getTextContent(key.name(), node);
					if (value != null && value.length() > 0) {
						item.put(key, value);
					}
				}

				banners.add(new BannerDescriptor(item));
			} catch (Exception e) {
				// log and ignore
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid banner descriptor", e);
			}
		}

		getCache().putData("banners", series.seriesId, null, banners.toArray(new BannerDescriptor[0]));
		return banners;
	}

	public static class BannerDescriptor implements Serializable {

		public static enum BannerProperty {
			id, BannerMirror, BannerPath, BannerType, BannerType2, Season, Colors, Language, Rating, RatingCount, SeriesName, ThumbnailPath, VignettePath
		}

		protected Map<BannerProperty, String> fields;

		protected BannerDescriptor() {
			// used by serializer
		}

		protected BannerDescriptor(Map<BannerProperty, String> fields) {
			this.fields = new EnumMap<BannerProperty, String>(fields);
		}

		public String get(Object key) {
			return fields.get(BannerProperty.valueOf(key.toString()));
		}

		public String get(BannerProperty key) {
			return fields.get(key);
		}

		public URL getBannerMirrorUrl() throws MalformedURLException {
			try {
				return new URL(get(BannerProperty.BannerMirror));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getUrl() throws MalformedURLException {
			try {
				return new URL(getBannerMirrorUrl(), get(BannerProperty.BannerPath));
			} catch (Exception e) {
				return null;
			}
		}

		public String getExtension() {
			return FileUtilities.getExtension(get(BannerProperty.BannerPath));
		}

		public Integer getId() {
			try {
				return new Integer(get(BannerProperty.id));
			} catch (Exception e) {
				return null;
			}
		}

		public String getBannerType() {
			return get(BannerProperty.BannerType);
		}

		public String getBannerType2() {
			return get(BannerProperty.BannerType2);
		}

		public Integer getSeason() {
			try {
				return new Integer(get(BannerProperty.Season));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		public String getColors() {
			return get(BannerProperty.Colors);
		}

		public Locale getLocale() {
			try {
				return new Locale(get(BannerProperty.Language));
			} catch (Exception e) {
				return null;
			}
		}

		public Double getRating() {
			try {
				return new Double(get(BannerProperty.Rating));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getRatingCount() {
			try {
				return new Integer(get(BannerProperty.RatingCount));
			} catch (Exception e) {
				return null;
			}
		}

		public boolean hasSeriesName() {
			return Boolean.parseBoolean(get(BannerProperty.SeriesName));
		}

		public URL getThumbnailUrl() throws MalformedURLException {
			try {
				return new URL(getBannerMirrorUrl(), get(BannerProperty.ThumbnailPath));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getVignetteUrl() throws MalformedURLException {
			try {
				return new URL(getBannerMirrorUrl(), get(BannerProperty.VignettePath));
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return fields.toString();
		}
	}

}
