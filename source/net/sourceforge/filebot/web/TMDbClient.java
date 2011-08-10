
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;


public class TMDbClient implements MovieIdentificationService {
	
	private static final String host = "api.themoviedb.org";
	private static final String version = "2.1";
	
	private final String apikey;
	

	public TMDbClient(String apikey) {
		this.apikey = apikey;
	}
	

	public String getName() {
		return "TheMovieDB";
	}
	

	public Icon getIcon() {
		return ResourceManager.getIcon("search.themoviedb");
	}
	

	@Override
	public List<MovieDescriptor> searchMovie(String query, Locale locale) throws IOException {
		try {
			return getMovies("Movie.search", query, locale);
		} catch (SAXException e) {
			// TMDb output is sometimes malformed xml
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
			return Collections.emptyList();
		}
	}
	

	public List<MovieDescriptor> searchMovie(File file, Locale locale) throws IOException, SAXException {
		return searchMovie(OpenSubtitlesHasher.computeHash(file), file.length(), locale);
	}
	

	public List<MovieDescriptor> searchMovie(String hash, long bytesize, Locale locale) throws IOException, SAXException {
		return getMovies("Media.getInfo", hash + "/" + bytesize, locale);
	}
	

	@Override
	public MovieDescriptor getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		URL resource = getResource("Movie.imdbLookup", String.format("tt%07d", imdbid), locale);
		Node movie = selectNode("//movie", getDocument(resource));
		
		if (movie == null)
			return null;
		
		String name = getTextContent("name", movie);
		int year = new Scanner(getTextContent("released", movie)).useDelimiter("\\D+").nextInt();
		
		return new MovieDescriptor(name, year, imdbid);
	}
	

	@Override
	public MovieDescriptor[] getMovieDescriptors(File[] movieFiles, Locale locale) throws Exception {
		MovieDescriptor[] movies = new MovieDescriptor[movieFiles.length];
		
		for (int i = 0; i < movies.length; i++) {
			List<MovieDescriptor> options = searchMovie(movieFiles[i], locale);
			
			// just use first result, if possible
			movies[i] = options.isEmpty() ? null : options.get(0);
		}
		
		return movies;
	}
	

	protected List<MovieDescriptor> getMovies(String method, String parameter, Locale locale) throws IOException, SAXException {
		List<MovieDescriptor> result = new ArrayList<MovieDescriptor>();
		
		for (Node node : selectNodes("//movie", getDocument(getResource(method, parameter, locale)))) {
			try {
				String name = getTextContent("name", node);
				
				// release date format will be YYYY-MM-DD, but we only care about the year
				int year = new Scanner(getTextContent("released", node)).useDelimiter("\\D+").nextInt();
				
				// imdb id will be tt1234567, but we only care about the number
				int imdbid = new Scanner(getTextContent("imdb_id", node)).useDelimiter("\\D+").nextInt();
				
				result.add(new MovieDescriptor(name, year, imdbid));
			} catch (RuntimeException e) {
				// release date or imdb id are undefined
			}
		}
		
		return result;
	}
	

	protected URL getResource(String method, String parameter, Locale locale) throws MalformedURLException {
		// e.g. http://api.themoviedb.org/2.1/Movie.search/en/xml/{apikey}/serenity
		return new URL("http", host, "/" + version + "/" + method + "/" + locale.getLanguage() + "/xml/" + apikey + "/" + parameter);
	}
	
}
