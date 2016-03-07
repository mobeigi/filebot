package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.util.XPathUtilities.*;
import static net.filebot.web.EpisodeUtilities.*;
import static net.filebot.web.WebRequest.*;

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

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.ResourceManager;
import net.filebot.util.FileUtilities;
import net.filebot.web.TheTVDBClient.BannerDescriptor.BannerProperty;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TheTVDBClient extends AbstractEpisodeListProvider {

	private final Map<MirrorType, String> mirrors = MirrorType.newMap();

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
	public boolean hasSeasonSupport() {
		return true;
	}

	@Override
	protected SortOrder vetoRequestParameter(SortOrder order) {
		return order != null ? order : SortOrder.Airdate;
	}

	@Override
	protected Locale vetoRequestParameter(Locale language) {
		return language != null ? language : Locale.ENGLISH;
	}

	@Override
	public ResultCache getCache() {
		return new ResultCache(getName(), Cache.getCache(getName(), CacheType.Daily));
	}

	public String getLanguageCode(Locale locale) {
		String code = locale.getLanguage();

		// sanity check
		if (code.length() != 2) {
			// see http://thetvdb.com/api/BA864DEE427E384A/languages.xml
			throw new IllegalArgumentException("Expecting 2-letter language code: " + code);
		}

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
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception {
		// perform online search
		Document dom = getXmlResource(MirrorType.SEARCH, "GetSeries.php?seriesname=" + encode(query, true) + "&language=" + getLanguageCode(locale));

		List<Node> nodes = selectNodes("Data/Series", dom);
		Map<Integer, TheTVDBSearchResult> resultSet = new LinkedHashMap<Integer, TheTVDBSearchResult>();

		for (Node node : nodes) {
			int sid = matchInteger(getTextContent("seriesid", node));
			String seriesName = getTextContent("SeriesName", node);

			if (seriesName.startsWith("**") && seriesName.endsWith("**")) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, String.format("Invalid series: %s [%d]", seriesName, sid));
				continue;
			}

			List<String> aliasNames = new ArrayList<String>();
			for (Node aliasNode : selectNodes("AliasNames", node)) {
				for (String aliasName : getTextContent(aliasNode).split("\\|")) {
					if (aliasName.trim().length() > 0) {
						aliasNames.add(aliasName.trim());
					}
				}
			}

			if (!resultSet.containsKey(sid)) {
				resultSet.put(sid, new TheTVDBSearchResult(seriesName, aliasNames.toArray(new String[0]), sid));
			}
		}

		return new ArrayList<SearchResult>(resultSet.values());
	}

	@Override
	protected SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
		TheTVDBSearchResult series = (TheTVDBSearchResult) searchResult;
		Document dom = getXmlResource(MirrorType.XML, "series/" + series.getSeriesId() + "/all/" + getLanguageCode(locale) + ".xml");

		// parse series info
		Node seriesNode = selectNode("Data/Series", dom);
		TheTVDBSeriesInfo seriesInfo = new TheTVDBSeriesInfo(getName(), sortOrder, locale, series.getId());
		seriesInfo.setAliasNames(searchResult.getEffectiveNames());

		seriesInfo.setName(getTextContent("SeriesName", seriesNode));
		seriesInfo.setAirsDayOfWeek(getTextContent("Airs_DayOfWeek", seriesNode));
		seriesInfo.setAirTime(getTextContent("Airs_Time", seriesNode));
		seriesInfo.setCertification(getTextContent("ContentRating", seriesNode));
		seriesInfo.setImdbId(getTextContent("IMDB_ID", seriesNode));
		seriesInfo.setNetwork(getTextContent("Network", seriesNode));
		seriesInfo.setOverview(getTextContent("Overview", seriesNode));
		seriesInfo.setStatus(getTextContent("Status", seriesNode));

		seriesInfo.setRating(getDecimal(getTextContent("Rating", seriesNode)));
		seriesInfo.setRatingCount(matchInteger(getTextContent("RatingCount", seriesNode)));
		seriesInfo.setRuntime(matchInteger(getTextContent("Runtime", seriesNode)));
		seriesInfo.setActors(getListContent("Actors", "\\|", seriesNode));
		seriesInfo.setGenres(getListContent("Genre", "\\|", seriesNode));
		seriesInfo.setStartDate(SimpleDate.parse(getTextContent("FirstAired", seriesNode)));

		seriesInfo.setBannerUrl(getResource(MirrorType.BANNER, getTextContent("banner", seriesNode)));
		seriesInfo.setFanartUrl(getResource(MirrorType.BANNER, getTextContent("fanart", seriesNode)));
		seriesInfo.setPosterUrl(getResource(MirrorType.BANNER, getTextContent("poster", seriesNode)));

		// parse episode data
		List<Node> nodes = selectNodes("Data/Episode", dom);

		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		List<Episode> specials = new ArrayList<Episode>(5);

		for (Node node : nodes) {
			String episodeName = getTextContent("EpisodeName", node);
			Integer absoluteNumber = matchInteger(getTextContent("absolute_number", node));
			SimpleDate airdate = SimpleDate.parse(getTextContent("FirstAired", node));

			// default numbering
			Integer episodeNumber = matchInteger(getTextContent("EpisodeNumber", node));
			Integer seasonNumber = matchInteger(getTextContent("SeasonNumber", node));

			// adjust for DVD numbering if possible
			if (sortOrder == SortOrder.DVD) {
				Integer dvdSeasonNumber = matchInteger(getTextContent("DVD_season", node));
				Integer dvdEpisodeNumber = matchInteger(getTextContent("DVD_episodenumber", node));

				// require both values to be valid integer numbers
				if (dvdSeasonNumber != null && dvdEpisodeNumber != null) {
					seasonNumber = dvdSeasonNumber;
					episodeNumber = dvdEpisodeNumber;
				}
			}

			// adjust for special numbering if necessary
			if (seasonNumber == null || seasonNumber == 0) {
				// handle as special episode
				for (String specialSeasonTag : new String[] { "airsafter_season", "airsbefore_season" }) {
					Integer specialSeason = matchInteger(getTextContent(specialSeasonTag, node));
					if (specialSeason != null && specialSeason != 0) {
						seasonNumber = specialSeason;
						break;
					}
				}

				// use given episode number as special number or count specials by ourselves
				Integer specialNumber = (episodeNumber != null) ? episodeNumber : filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesInfo.getName(), seasonNumber, null, episodeName, null, specialNumber, airdate, new SeriesInfo(seriesInfo)));
			} else {
				// adjust for absolute numbering if possible
				if (sortOrder == SortOrder.Absolute) {
					if (absoluteNumber != null && absoluteNumber > 0) {
						episodeNumber = absoluteNumber;
						seasonNumber = null;
					}
				}

				// handle as normal episode
				episodes.add(new Episode(seriesInfo.getName(), seasonNumber, episodeNumber, episodeName, absoluteNumber, null, airdate, new SeriesInfo(seriesInfo)));
			}
		}

		// episodes my not be ordered by DVD episode number
		sort(episodes, episodeComparator());

		// add specials at the end
		episodes.addAll(specials);

		return new SeriesData(seriesInfo, episodes);
	}

	public TheTVDBSearchResult lookupByID(int id, Locale locale) throws Exception {
		TheTVDBSearchResult cachedItem = getCache().getData("lookupByID", id, locale, TheTVDBSearchResult.class);
		if (cachedItem != null) {
			return cachedItem;
		}

		Document dom = getXmlResource(MirrorType.XML, "series/" + id + "/all/" + getLanguageCode(locale) + ".xml");
		String name = selectString("//SeriesName", dom);

		TheTVDBSearchResult series = new TheTVDBSearchResult(name, id);
		getCache().putData("lookupByID", id, locale, series);
		return series;
	}

	public TheTVDBSearchResult lookupByIMDbID(int imdbid, Locale locale) throws Exception {
		if (imdbid <= 0) {
			throw new IllegalArgumentException("id must not be " + imdbid);
		}

		TheTVDBSearchResult cachedItem = getCache().getData("lookupByIMDbID", imdbid, locale, TheTVDBSearchResult.class);
		if (cachedItem != null) {
			return cachedItem;
		}

		Document dom = getXmlResource(MirrorType.SEARCH, "GetSeriesByRemoteID.php?imdbid=" + imdbid + "&language=" + getLanguageCode(locale));

		String id = selectString("//seriesid", dom);
		String name = selectString("//SeriesName", dom);

		if (id == null || id.isEmpty() || name == null || name.isEmpty())
			return null;

		TheTVDBSearchResult series = new TheTVDBSearchResult(name, Integer.parseInt(id));
		getCache().putData("lookupByIMDbID", imdbid, locale, series);
		return series;
	}

	protected String getMirror(MirrorType mirrorType) throws Exception {
		// use default server
		if (mirrorType == MirrorType.NULL) {
			return "http://thetvdb.com";
		}

		synchronized (mirrors) {
			// initialize mirrors
			if (mirrors.isEmpty()) {
				Document dom = getXmlResource(MirrorType.NULL, "mirrors.xml");

				// collect all mirror data
				Map<MirrorType, List<String>> mirrorLists = selectNodes("Mirrors/Mirror", dom).stream().flatMap(node -> {
					String mirror = getTextContent("mirrorpath", node);
					int typeMask = Integer.parseInt(getTextContent("typemask", node));

					return MirrorType.fromTypeMask(typeMask).stream().collect(toMap(m -> m, m -> mirror)).entrySet().stream();
				}).collect(groupingBy(Entry::getKey, MirrorType::newMap, mapping(Entry::getValue, toList())));

				// select random mirror for each type
				Random random = new Random();

				mirrorLists.forEach((type, options) -> {
					String selection = options.get(random.nextInt(options.size()));
					mirrors.put(type, selection);
				});
			}

			// return selected mirror
			return mirrors.get(mirrorType);
		}
	}

	protected Document getXmlResource(MirrorType mirror, String resource) throws Exception {
		Cache cache = Cache.getCache(getName(), CacheType.Monthly);
		return cache.xml(resource, s -> getResource(mirror, s), Cache.ONE_DAY).get();
	}

	protected URL getResource(MirrorType mirror, String path) throws Exception {
		StringBuilder url = new StringBuilder(getMirror(mirror)).append('/').append(mirror.prefix()).append('/');
		if (mirror.keyRequired()) {
			url.append(apikey).append('/');
		}
		return new URL(url.append(path).toString());
	}

	protected static enum MirrorType {

		NULL(0), SEARCH(1), XML(1), BANNER(2);

		final int bitMask;

		private MirrorType(int bitMask) {
			this.bitMask = bitMask;
		}

		public String prefix() {
			return this != BANNER ? "api" : "banners";
		}

		public boolean keyRequired() {
			return this != BANNER && this != SEARCH;
		}

		public static EnumSet<MirrorType> fromTypeMask(int mask) {
			// convert bit mask to enumset
			return EnumSet.of(SEARCH, XML, BANNER).stream().filter(m -> {
				return (mask & m.bitMask) != 0;
			}).collect(toCollection(MirrorType::newSet));
		};

		public static EnumSet<MirrorType> newSet() {
			return EnumSet.noneOf(MirrorType.class);
		}

		public static <T> EnumMap<MirrorType, T> newMap() {
			return new EnumMap<MirrorType, T>(MirrorType.class);
		}

	}

	public SeriesInfo getSeriesInfoByIMDbID(int imdbid, Locale locale) throws Exception {
		return getSeriesInfo(lookupByIMDbID(imdbid, locale), locale);
	}

	@Override
	protected SearchResult createSearchResult(int id) {
		return new TheTVDBSearchResult(null, id);
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create("http://www.thetvdb.com/?tab=seasonall&id=" + ((TheTVDBSearchResult) searchResult).getSeriesId());
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
		BannerDescriptor[] cachedList = getCache().getData("banners", series.getId(), null, BannerDescriptor[].class);
		if (cachedList != null) {
			return asList(cachedList);
		}

		Document dom = getXmlResource(MirrorType.XML, "series/" + series.getId() + "/banners.xml");

		List<Node> nodes = selectNodes("//Banner", dom);
		List<BannerDescriptor> banners = new ArrayList<BannerDescriptor>();

		for (Node node : nodes) {
			try {
				Map<BannerProperty, String> item = new EnumMap<BannerProperty, String>(BannerProperty.class);

				// insert banner mirror
				item.put(BannerProperty.BannerMirror, getResource(MirrorType.BANNER, "").toString());

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

		getCache().putData("banners", series.getId(), null, banners.toArray(new BannerDescriptor[0]));
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
