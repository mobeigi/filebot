
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo.MovieProperty;
import net.sourceforge.filebot.web.TMDbClient.Person;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class IMDbClient implements MovieIdentificationService {
	
	private final String host = "akas.imdb.com";
	
	
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
		Document dom = parsePage(new URL("http", host, "/find?s=tt&q=" + encode(query)));
		
		// select movie links followed by year in parenthesis
		List<Node> nodes = selectNodes("//TABLE//A[substring-after(substring-before(following::text(),')'),'(')]", dom);
		List<Movie> results = new ArrayList<Movie>(nodes.size());
		
		for (Node node : nodes) {
			try {
				String name = node.getTextContent().trim();
				if (name.startsWith("\""))
					continue;
				
				String year = node.getNextSibling().getTextContent().replaceAll("[\\p{Punct}\\p{Space}]+", ""); // remove non-number characters
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
				if (movie == null) {
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
			String header = selectString("//H1", dom).toUpperCase();
			if (header.contains("(VG)")) // ignore video games and videos
				return null;
			
			String name = selectString("//H1/A/text()", dom).replaceAll("\\s+", " ").trim();
			String year = new Scanner(selectString("//H1/A/following::A/text()", dom)).useDelimiter("\\D+").next();
			String url = selectString("//H1/A/@href", dom);
			
			// try to get localized name
			if (locale != null && locale != Locale.ROOT) {
				try {
					String language = String.format("(%s title)", locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase());
					List<Node> akaRows = selectNodes("//*[@name='akas']//following::TABLE[1]//TR", dom);
					
					for (Node aka : akaRows) {
						List<Node> columns = getChildren("TD", aka);
						String akaTitle = getTextContent(columns.get(0));
						String languageDesc = getTextContent(columns.get(1)).toLowerCase();
						
						if (language.length() > 0 && languageDesc.contains(language) && languageDesc.contains("international")) {
							name = akaTitle;
							break;
						}
					}
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to grep localized name: " + name);
				}
			}
			
			return new Movie(name, Pattern.matches("\\d{4}", year) ? Integer.parseInt(year) : -1, getImdbId(url), -1);
		} catch (Exception e) {
			// ignore, we probably got redirected to an error page
			return null;
		}
	}
	
	
	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		try {
			return scrapeMovie(parsePage(new URL("http", host, String.format("/title/tt%07d/releaseinfo", imdbid))), locale);
		} catch (FileNotFoundException e) {
			return null; // illegal imdbid
		}
	}
	
	
	protected Document parsePage(URL url) throws IOException, SAXException {
		CachedPage page = new CachedPage(url) {
			
			@Override
			protected Reader openConnection(URL url) throws IOException {
				URLConnection connection = url.openConnection();
				
				// IMDb refuses default user agent (Java/1.6.0_12)
				connection.addRequestProperty("User-Agent", "Mozilla");
				
				return getReader(connection);
			}
		};
		
		return getHtmlDocument(page.get());
	}
	
	
	public URI getMoviePageLink(int imdbId) {
		return URI.create(String.format("http://www.imdb.com/title/tt%07d/", imdbId));
	}
	
	
	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}
	
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getImdbApiData(Integer i, String t, String y) throws IOException {
		String url = i != null ? String.format("http://www.deanclatworthy.com/imdb/?id=tt%07d", i) : String.format("http://www.deanclatworthy.com/imdb/?q=%s&year=%s", encode(t), encode(y));
		CachedResource<JSONObject> data = new CachedResource<JSONObject>(url, JSONObject.class, 7 * 24 * 60 * 60 * 1000) {
			
			@Override
			public JSONObject process(ByteBuffer data) throws Exception {
				return (JSONObject) JSONValue.parse(Charset.forName("UTF-8").decode(data).toString());
			}
			
			
			@Override
			protected Cache getCache() {
				return CacheManager.getInstance().getCache("web-data-diskcache");
			}
		};
		
		return data.get();
	}
	
	
	public MovieInfo getImdbApiMovieInfo(Movie movie) throws IOException {
		Map<String, String> data = movie.getImdbId() > 0 ? getImdbApiData(movie.getImdbId(), null, null) : getImdbApiData(null, movie.getName(), String.valueOf(movie.getYear()));
		
		// sanity check
		if (data.get("error") != null) {
			throw new IllegalArgumentException(data.get("error"));
		}
		
		Map<MovieProperty, String> fields = new EnumMap<MovieProperty, String>(MovieProperty.class);
		fields.put(MovieProperty.vote_average, data.get("rating"));
		fields.put(MovieProperty.vote_count, data.get("votes"));
		fields.put(MovieProperty.imdb_id, data.get("imdbid"));
		
		return new MovieInfo(fields, new ArrayList<String>(0), new ArrayList<String>(0), new ArrayList<Person>(0));
	}
}
