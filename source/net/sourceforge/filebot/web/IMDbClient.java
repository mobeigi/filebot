package net.sourceforge.filebot.web;

import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo.MovieProperty;
import net.sourceforge.filebot.web.TMDbClient.Person;
import net.sourceforge.filebot.web.TMDbClient.Trailer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class IMDbClient implements MovieIdentificationService {

	private String host = "www.imdb.com";

	@Override
	public String getName() {
		return "IMDb";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.imdb");
	}

	protected int getImdbId(String link) {
		Matcher matcher = Pattern.compile("tt(\\d{7})").matcher(link);

		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}

		// pattern not found
		throw new IllegalArgumentException(String.format("Cannot find imdb id: %s", link));
	}

	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		Document dom = parsePage(new URL("http", host, "/find?s=tt&q=" + encode(query, false)));

		// select movie links followed by year in parenthesis
		List<Node> nodes = selectNodes("//TABLE[@class='findList']//TD/A[substring-after(substring-before(following::text(),')'),'(')]", dom);
		List<Movie> results = new ArrayList<Movie>(nodes.size());

		for (Node node : nodes) {
			try {
				String name = node.getTextContent().trim();
				if (name.startsWith("\""))
					continue;

				String year = node.getNextSibling().getTextContent().trim().replaceFirst("^\\(I\\)", "").replaceAll("[\\p{Punct}\\p{Space}]+", ""); // remove non-number characters
				String href = getAttribute("href", node);

				results.add(new Movie(name, Integer.parseInt(year), getImdbId(href), -1));
			} catch (Exception e) {
				// ignore illegal movies (TV Shows, Videos, Video Games, etc)
			}
		}

		// we might have been redirected to the movie page
		if (results.isEmpty()) {
			try {
				int imdbid = getImdbId(selectString("//LINK[@rel='canonical']/@href", dom));
				Movie movie = getMovieDescriptor(imdbid, locale);
				if (movie != null) {
					results.add(movie);
				}
			} catch (Exception e) {
				// ignore, can't find movie
			}
		}

		return results;
	}

	protected Movie scrapeMovie(Document dom, Locale locale) {
		try {
			int imdbid = getImdbId(selectString("//LINK[@rel='canonical']/@href", dom));
			String title = selectString("//META[@property='og:title']/@content", dom);

			Matcher titleMatcher = Pattern.compile("(.+)\\s\\((?i:tv.|video.)?(\\d{4})\\)$").matcher(title);
			if (!titleMatcher.matches())
				return null;

			return new Movie(titleMatcher.group(1), Integer.parseInt(titleMatcher.group(2)), imdbid, -1);
		} catch (Exception e) {
			// ignore, we probably got redirected to an error page
			return null;
		}
	}

	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		try {
			return scrapeMovie(parsePage(new URL("http", host, String.format("/title/tt%07d/", imdbid))), locale);
		} catch (FileNotFoundException e) {
			return null; // illegal imdbid
		}
	}

	protected Document parsePage(URL url) throws IOException, SAXException {
		CachedPage page = new CachedPage(url) {

			@Override
			protected Reader openConnection(URL url) throws IOException {
				URLConnection connection = url.openConnection();

				// IMDb refuses default user agent (Java/1.6.0_12) => SPOOF GOOGLEBOT
				connection.addRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
				connection.addRequestProperty("From", "googlebot(at)googlebot.com");
				connection.addRequestProperty("Accept", "*/*");
				connection.addRequestProperty("X-Forwarded-For", "66.249.73.100"); // TRICK ANNOYING IMDB GEO-LOCATION LOCALIZATION

				return getReader(connection);
			}
		};

		return getHtmlDocument(page.get());
	}

	public String scrape(String imdbid, String xpath) throws IOException, SAXException {
		return scrape(getMoviePageLink(getImdbId(imdbid)).toURL(), xpath); // helper for scraping data in user scripts
	}

	public String scrape(URL url, String xpath) throws IOException, SAXException {
		return selectString(xpath, parsePage(url)); // helper for scraping data in user scripts
	}

	public URI getMoviePageLink(int imdbId) {
		return URI.create(String.format("http://www.imdb.com/title/tt%07d/", imdbId));
	}

	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, String> getImdbApiData(Integer i, String t, String y, boolean tomatoes) throws IOException {
		// e.g. http://www.imdbapi.com/?i=tt0379786&r=xml&tomatoes=true
		String url = String.format("http://www.omdbapi.com/?i=%s&t=%s&y=%s&r=xml&tomatoes=%s", String.format(i == null ? "" : "tt%07d", i), t, y, tomatoes);
		CachedResource<HashMap> data = new CachedResource<HashMap>(url, HashMap.class) {

			@Override
			public HashMap process(ByteBuffer data) throws Exception {
				Document xml = getDocument(Charset.forName("UTF-8").decode(data).toString());
				HashMap attr = new HashMap();
				for (Node it : selectNodes("//@*", xml)) {
					attr.put(it.getNodeName(), it.getTextContent());
				}
				return attr;
			}

			@Override
			protected Cache getCache() {
				return CacheManager.getInstance().getCache("web-datasource-lv2");
			}
		};

		return data.get();
	}

	public MovieInfo getImdbApiMovieInfo(Movie movie) throws IOException {
		Map<String, String> data = movie.getImdbId() > 0 ? getImdbApiData(movie.getImdbId(), "", "", false) : getImdbApiData(null, movie.getName(), String.valueOf(movie.getYear()), false);

		// sanity check
		if (!Boolean.parseBoolean(data.get("response"))) {
			throw new IllegalArgumentException("Movie not found: " + data);
		}

		Map<MovieProperty, String> fields = new EnumMap<MovieProperty, String>(MovieProperty.class);
		fields.put(MovieProperty.title, data.get("title"));
		fields.put(MovieProperty.certification, data.get("rated"));
		fields.put(MovieProperty.runtime, data.get("runtime"));
		fields.put(MovieProperty.tagline, data.get("plot"));
		fields.put(MovieProperty.vote_average, data.get("imdbRating"));
		fields.put(MovieProperty.vote_count, data.get("imdbVotes").replaceAll("\\D", ""));
		fields.put(MovieProperty.imdb_id, data.get("imdbID"));
		fields.put(MovieProperty.poster_path, data.get("poster"));

		// convert release date to yyyy-MM-dd
		Date released = Date.parse(data.get("released"), "dd MMM yyyy");
		if (released != null) {
			fields.put(MovieProperty.release_date, released.format("yyyy-MM-dd"));
		} else {
			Date year = Date.parse(data.get("year"), "yyyy");
			if (year != null) {
				fields.put(MovieProperty.release_date, year.format("yyyy-MM-dd"));
			}
		}

		List<String> genres = new ArrayList<String>();
		for (String it : data.get("genre").split(",")) {
			genres.add(it.trim());
		}

		List<Person> actors = new ArrayList<Person>();
		for (String it : data.get("actors").split(",")) {
			actors.add(new Person(it.trim(), null, null));
		}

		for (String director : data.get("director").split(",")) {
			actors.add(new Person(director, null, "Director"));
		}

		for (String writer : data.get("writer").split(",")) {
			actors.add(new Person(writer, null, "Writer"));
		}

		return new MovieInfo(fields, new ArrayList<String>(0), genres, new ArrayList<String>(0), actors, new ArrayList<Trailer>(0));
	}
}
