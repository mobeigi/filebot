
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TMDbClient implements MovieIdentificationService {
	
	private static final String host = "api.themoviedb.org";
	private static final String version = "2.1";
	
	private final String apikey;
	

	public TMDbClient(String apikey) {
		this.apikey = apikey;
	}
	

	public List<MovieDescriptor> searchMovie(String query) throws IOException, SAXException {
		return getMovies("Movie.search", query);
	}
	

	public List<MovieDescriptor> searchMovie(File file) throws IOException, SAXException {
		return getMovies("Hash.getInfo", OpenSubtitlesHasher.computeHash(file));
	}
	

	@Override
	public MovieDescriptor[] getMovieDescriptors(File[] movieFiles) throws Exception {
		MovieDescriptor[] movies = new MovieDescriptor[movieFiles.length];
		
		for (int i = 0; i < movies.length; i++) {
			List<MovieDescriptor> options = searchMovie(movieFiles[i]);
			
			// just use first result, if possible
			movies[i] = options.isEmpty() ? null : options.get(0);
		}
		
		return movies;
	}
	

	protected List<MovieDescriptor> getMovies(String method, String parameter) throws IOException, SAXException {
		List<MovieDescriptor> result = new ArrayList<MovieDescriptor>();
		
		for (Node node : selectNodes("//movie", getDocument(getResource(method, parameter)))) {
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
	

	protected URL getResource(String method, String parameter) throws MalformedURLException {
		// e.g. http://api.themoviedb.org/2.1/Movie.search/en/xml/{apikey}/serenity
		return new URL("http", host, "/" + version + "/" + method + "/en/xml/" + apikey + "/" + parameter);
	}
	
}
