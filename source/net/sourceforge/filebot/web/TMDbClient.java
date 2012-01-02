
package net.sourceforge.filebot.web;


import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TMDbClient.Artwork.ArtworkProperty;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo.MovieProperty;


public class TMDbClient implements MovieIdentificationService {
	
	private static final String host = "api.themoviedb.org";
	private static final String version = "2.1";
	
	private final String apikey;
	
	
	public TMDbClient(String apikey) {
		this.apikey = apikey;
	}
	
	
	@Override
	public String getName() {
		return "TheMovieDB";
	}
	
	
	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.themoviedb");
	}
	
	
	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws IOException {
		try {
			return getMovies("Movie.search", encode(query), locale);
		} catch (SAXException e) {
			// TMDb output is sometimes malformed xml
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
			return emptyList();
		}
	}
	
	
	public List<Movie> searchMovie(File file, Locale locale) throws IOException, SAXException {
		return emptyList(); // API BROKEN
		// return searchMovie(OpenSubtitlesHasher.computeHash(file), file.length(), locale); 
	}
	
	
	public List<Movie> searchMovie(String hash, long bytesize, Locale locale) throws IOException, SAXException {
		return getMovies("Media.getInfo", hash + "/" + bytesize, locale);
	}
	
	
	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		URL resource = getResource("Movie.imdbLookup", String.format("tt%07d", imdbid), locale);
		Node movie = selectNode("//movie", getDocument(resource));
		
		if (movie == null)
			return null;
		
		String name = getTextContent("name", movie);
		int year = new Scanner(getTextContent("released", movie)).useDelimiter("\\D+").nextInt();
		
		return new Movie(name, year, imdbid);
	}
	
	
	@Override
	public Movie[] getMovieDescriptors(File[] movieFiles, Locale locale) throws Exception {
		Movie[] movies = new Movie[movieFiles.length];
		
		for (int i = 0; i < movies.length; i++) {
			List<Movie> options = searchMovie(movieFiles[i], locale);
			
			// just use first result, if possible
			movies[i] = options.isEmpty() ? null : options.get(0);
		}
		
		return movies;
	}
	
	
	protected List<Movie> getMovies(String method, String parameter, Locale locale) throws IOException, SAXException {
		List<Movie> result = new ArrayList<Movie>();
		
		for (Node node : selectNodes("//movie", getDocument(getResource(method, parameter, locale)))) {
			try {
				String name = getTextContent("name", node);
				
				// release date format will be YYYY-MM-DD, but we only care about the year
				int year = new Scanner(getTextContent("released", node)).useDelimiter("\\D+").nextInt();
				
				// imdb id will be tt1234567, but we only care about the number
				int imdbid = new Scanner(getTextContent("imdb_id", node)).useDelimiter("\\D+").nextInt();
				
				result.add(new Movie(name, year, imdbid));
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
	
	
	public MovieInfo getMovieInfo(Movie movie, Locale locale) throws Exception {
		URL resource = getResource("Movie.imdbLookup", String.format("tt%07d", movie.getImdbId()), locale);
		Node node = selectNode("//movie", getDocument(resource));
		
		Map<MovieProperty, String> movieProperties = new EnumMap<MovieProperty, String>(MovieProperty.class);
		for (MovieProperty property : MovieProperty.values()) {
			movieProperties.put(property, getTextContent(property.name(), node));
		}
		
		List<String> genres = new ArrayList<String>();
		for (Node category : selectNodes("//category[@type='genre']", node)) {
			genres.add(getAttribute("name", category));
		}
		
		List<Artwork> artwork = new ArrayList<Artwork>();
		for (Node image : selectNodes("//image", node)) {
			Map<ArtworkProperty, String> artworkProperties = new EnumMap<ArtworkProperty, String>(ArtworkProperty.class);
			for (ArtworkProperty property : ArtworkProperty.values()) {
				artworkProperties.put(property, getAttribute(property.name(), image));
			}
			artwork.add(new Artwork(artworkProperties));
		}
		
		return new MovieInfo(movieProperties, genres, artwork);
	}
	
	
	public static class MovieInfo implements Serializable {
		
		public static enum MovieProperty {
			translated,
			adult,
			language,
			name,
			type,
			id,
			imdb_id,
			url,
			overview,
			votes,
			rating,
			certification,
			released,
			runtime
		}
		
		
		protected Map<MovieProperty, String> fields;
		protected String[] genres;
		protected Artwork[] images;
		
		
		protected MovieInfo() {
			// used by serializer
		}
		
		
		protected MovieInfo(Map<MovieProperty, String> fields, List<String> genres, List<Artwork> images) {
			this.fields = new EnumMap<MovieProperty, String>(fields);
			this.genres = genres.toArray(new String[0]);
			this.images = images.toArray(new Artwork[0]);
		}
		
		
		public String get(Object key) {
			return fields.get(MovieProperty.valueOf(key.toString()));
		}
		
		
		public String get(MovieProperty key) {
			return fields.get(key);
		}
		
		
		public boolean isTranslated() {
			return Boolean.valueOf(get(MovieProperty.translated));
		}
		
		
		public boolean isAdult() {
			return Boolean.valueOf(get(MovieProperty.adult));
		}
		
		
		public Locale getLanguage() {
			try {
				return new Locale(get(MovieProperty.language));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public String getName() {
			return get(MovieProperty.name);
		}
		
		
		public String getType() {
			return get(MovieProperty.type);
		}
		
		
		public Integer getId() {
			try {
				return new Integer(get(MovieProperty.id));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public Integer getImdbId() {
			// e.g. tt0379786
			try {
				return new Integer(get(MovieProperty.imdb_id).substring(2));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public URL getUrl() {
			try {
				return new URL(get(MovieProperty.url));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public String getOverview() {
			return get(MovieProperty.overview);
		}
		
		
		public Integer getVotes() {
			try {
				return new Integer(get(MovieProperty.votes));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public Double getRating() {
			try {
				return new Double(get(MovieProperty.rating));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public String getCertification() {
			// e.g. PG-13
			return get(MovieProperty.certification);
		}
		
		
		public Date getReleased() {
			// e.g. 2005-09-30
			return Date.parse(get(MovieProperty.released), "yyyy-MM-dd");
		}
		
		
		public Integer getRuntime() {
			try {
				return new Integer(get(MovieProperty.runtime));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public List<String> getGenres() {
			return unmodifiableList(asList(genres));
		}
		
		
		public List<Artwork> getImages() {
			return unmodifiableList(asList(images));
		}
		
		
		@Override
		public String toString() {
			return fields.toString();
		}
	}
	
	
	public static class Artwork implements Serializable {
		
		public static enum ArtworkProperty {
			type,
			url,
			size,
			width,
			height
		}
		
		
		protected Map<ArtworkProperty, String> fields;
		
		
		protected Artwork() {
			// used by serializer
		}
		
		
		protected Artwork(Map<ArtworkProperty, String> fields) {
			this.fields = new EnumMap<ArtworkProperty, String>(fields);
		}
		
		
		public String get(Object key) {
			return fields.get(ArtworkProperty.valueOf(key.toString()));
		}
		
		
		public String get(ArtworkProperty key) {
			return fields.get(key);
		}
		
		
		public String getType() {
			return get(ArtworkProperty.type);
		}
		
		
		public URL getUrl() {
			try {
				return new URL(get(ArtworkProperty.url));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public String getSize() {
			return get(ArtworkProperty.size);
		}
		
		
		public Integer getWidth() {
			try {
				return new Integer(get(ArtworkProperty.width));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public Integer getHeight() {
			try {
				return new Integer(get(ArtworkProperty.height));
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
