package net.filebot.web;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.util.XPathUtilities.*;
import static net.filebot.web.EpisodeUtilities.*;
import static net.filebot.web.WebRequest.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.Icon;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.Resource;
import net.filebot.ResourceManager;

public class AnidbClient extends AbstractEpisodeListProvider {

	private static final FloodLimit REQUEST_LIMIT = new FloodLimit(2, 5, TimeUnit.SECONDS); // no more than 2 requests within a 5 second window

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
	public boolean hasSeasonSupport() {
		return false;
	}

	@Override
	protected SortOrder vetoRequestParameter(SortOrder order) {
		return SortOrder.Absolute;
	}

	@Override
	protected Cache getCache(String section) {
		return Cache.getCache(getName() + "_" + section, CacheType.Weekly);
	}

	@Override
	public List<SearchResult> search(String query, Locale locale) throws Exception {
		// bypass automatic caching since search is based on locally cached data anyway
		return fetchSearchResult(query, locale);
	}

	// local AniDB search index
	private final Resource<LocalSearch<SearchResult>> localIndex = Resource.lazy(() -> {
		return new LocalSearch<SearchResult>(getAnimeTitles(), SearchResult::getEffectiveNames);
	}).memoize();

	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception {
		return localIndex.get().search(query);
	}

	@Override
	protected SeriesData fetchSeriesData(SearchResult anime, SortOrder sortOrder, Locale locale) throws Exception {
		// e.g. http://api.anidb.net:9001/httpapi?request=anime&client=filebot&clientver=1&protover=1&aid=4521
		URL url = new URL("http://api.anidb.net:9001/httpapi?request=anime&client=" + client + "&clientver=" + clientver + "&protover=1&aid=" + anime.getId());

		// respect flood protection limits
		REQUEST_LIMIT.acquirePermit();

		// get anime page as xml
		Document dom = getDocument(url);

		// parse series info
		SeriesInfo seriesInfo = new SeriesInfo(this, sortOrder, locale, anime.getId());
		seriesInfo.setAliasNames(anime.getAliasNames());

		// AniDB types: Movie, Music Video, Other, OVA, TV Series, TV Special, Web, unknown
		String animeType = selectString("//type", dom);
		if (animeType != null && animeType.matches("(?i:music.video|unkown)")) {
			return new SeriesData(seriesInfo, emptyList());
		}

		seriesInfo.setName(selectString("anime/titles/title[@type='main']", dom));
		seriesInfo.setRating(getDecimal(selectString("anime/ratings/permanent", dom)));
		seriesInfo.setRatingCount(matchInteger(getTextContent("anime/ratings/permanent/@count", dom)));
		seriesInfo.setStartDate(SimpleDate.parse(selectString("anime/startdate", dom)));

		// add categories ordered by weight as genres
		// * only use categories with weight >= 400
		// * sort by weight (descending)
		// * limit to 5 genres
		seriesInfo.setGenres(streamNodes("anime/categories/category", dom).map(categoryNode -> {
			String name = getTextContent("name", categoryNode);
			Integer weight = matchInteger(getAttribute("weight", categoryNode));
			return new SimpleImmutableEntry<String, Integer>(name, weight);
		}).filter(nw -> {
			return nw.getKey() != null && nw.getValue() != null && nw.getKey().length() > 0 && nw.getValue() >= 400;
		}).sorted((a, b) -> {
			return b.getValue().compareTo(a.getValue());
		}).map(it -> it.getKey()).limit(5).collect(Collectors.toList()));

		// parse episode data
		String animeTitle = selectString("anime/titles/title[@type='official' and @lang='" + locale.getLanguage() + "']", dom);
		if (animeTitle == null || animeTitle.length() == 0) {
			animeTitle = seriesInfo.getName();
		}

		List<Episode> episodes = new ArrayList<Episode>(25);

		for (Node node : selectNodes("anime/episodes/episode", dom)) {
			Node epno = getChild("epno", node);
			int number = Integer.parseInt(getTextContent(epno).replaceAll("\\D", ""));
			int type = Integer.parseInt(getAttribute("type", epno));

			if (type == 1 || type == 2) {
				SimpleDate airdate = SimpleDate.parse(getTextContent("airdate", node));
				String title = selectString(".//title[@lang='" + locale.getLanguage() + "']", node);
				if (title.isEmpty()) { // English language fall-back
					title = selectString(".//title[@lang='en']", node);
				}

				if (type == 1) {
					episodes.add(new Episode(animeTitle, null, number, title, number, null, airdate, new SeriesInfo(seriesInfo))); // normal episode, no seasons for anime
				} else {
					episodes.add(new Episode(animeTitle, null, null, title, null, number, airdate, new SeriesInfo(seriesInfo))); // special episode
				}
			}
		}

		// make sure episodes are in ordered correctly
		episodes.sort(episodeComparator());

		// sanity check
		if (episodes.isEmpty()) {
			debug.fine(format("No episode data: %s (%d) => %s", anime, anime.getId(), url));
		}

		return new SeriesData(seriesInfo, episodes);
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		try {
			return new URI("http://anidb.net/a" + searchResult.getId());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This method is overridden in {@link net.filebot.WebServices.AnidbClientWithLocalSearch} to fetch the Anime Index from our own host and not anidb.net
	 */
	public synchronized SearchResult[] getAnimeTitles() throws Exception {
		// get data file (unzip and cache)
		byte[] bytes = getCache("root").bytes("anime-titles.dat.gz", n -> new URL("http://anidb.net/api/" + n)).get();

		// <aid>|<type>|<language>|<title>
		// type: 1=primary title (one per anime), 2=synonyms (multiple per anime), 3=shorttitles (multiple per anime), 4=official title (one per language)
		Pattern pattern = Pattern.compile("^(?!#)(\\d+)[|](\\d)[|]([\\w-]+)[|](.+)$");

		List<String> languageOrder = new ArrayList<String>();
		languageOrder.add("x-jat");
		languageOrder.add("en");
		languageOrder.add("ja");

		List<String> typeOrder = new ArrayList<String>();
		typeOrder.add("1");
		typeOrder.add("4");
		typeOrder.add("2");
		typeOrder.add("3");

		// fetch data
		Map<Integer, List<Object[]>> entriesByAnime = new HashMap<Integer, List<Object[]>>(65536);

		try (BufferedReader text = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), UTF_8))) {
			text.lines().forEach(line -> {
				Matcher matcher = pattern.matcher(line);

				if (matcher.matches()) {
					int aid = Integer.parseInt(matcher.group(1));
					String type = matcher.group(2);
					String language = matcher.group(3);
					String title = matcher.group(4);

					if (aid > 0 && title.length() > 0 && typeOrder.contains(type) && languageOrder.contains(language)) {
						// resolve HTML entities
						title = Jsoup.parse(title).text();

						if (type.equals("3") && (title.length() < 5 || !Character.isUpperCase(title.charAt(0)) || Character.isUpperCase(title.charAt(title.length() - 1)))) {
							return;
						}

						entriesByAnime.computeIfAbsent(aid, k -> new ArrayList<Object[]>()).add(new Object[] { typeOrder.indexOf(type), languageOrder.indexOf(language), title });
					}
				}
			});
		}

		// build up a list of all possible AniDB search results
		return entriesByAnime.entrySet().stream().map(it -> {
			List<String> names = it.getValue().stream().sorted((a, b) -> {
				for (int i = 0; i < a.length; i++) {
					if (!a[i].equals(b[i])) {
						return ((Comparable) a[i]).compareTo(b[i]);
					}
				}
				return 0;
			}).map(n -> n[2].toString()).collect(toList());

			String primaryTitle = names.get(0);
			List<String> aliasNames = names.subList(1, names.size());
			return new SearchResult(it.getKey(), primaryTitle, aliasNames);
		}).toArray(SearchResult[]::new);
	}

}
