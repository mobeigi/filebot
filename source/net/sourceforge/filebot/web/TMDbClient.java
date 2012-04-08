
package net.sourceforge.filebot.web;


import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TMDbClient.Artwork.ArtworkProperty;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo.MovieProperty;
import net.sourceforge.filebot.web.TMDbClient.Person.PersonProperty;


public class TMDbClient implements MovieIdentificationService {
	
	private static final String host = "api.themoviedb.org";
	private static final String version = "2.1";
	
	private static final FloodLimit SEARCH_LIMIT = new FloodLimit(10, 10, TimeUnit.SECONDS);
	private static final FloodLimit REQUEST_LIMIT = new FloodLimit(20, 10, TimeUnit.SECONDS);
	
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
			return getMovies("Movie.search", encode(query), locale, SEARCH_LIMIT);
		} catch (SAXException e) {
			// TMDb output is sometimes malformed xml
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
			return emptyList();
		}
	}
	
	
	public List<Movie> searchMovie(String hash, long bytesize, Locale locale) throws IOException, SAXException {
		return getMovies("Media.getInfo", hash + "/" + bytesize, locale, SEARCH_LIMIT);
	}
	
	
	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		Document dom = fetchResource("Movie.imdbLookup", String.format("tt%07d", imdbid), locale, REQUEST_LIMIT);
		Node movie = selectNode("//movie", dom);
		
		if (movie == null)
			return null;
		
		String name = getTextContent("name", movie);
		String released = getTextContent("released", movie);
		int year = -1;
		
		try {
			year = new Scanner(released).useDelimiter("\\D+").nextInt();
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal release year: " + released);
		}
		
		return new Movie(name, year, imdbid);
	}
	
	
	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}
	
	
	protected List<Movie> getMovies(String method, String parameter, Locale locale, FloodLimit limit) throws IOException, SAXException {
		Document dom = fetchResource(method, parameter, locale, limit);
		List<Movie> result = new ArrayList<Movie>();
		
		for (Node node : selectNodes("//movie", dom)) {
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
	
	
	protected URL getResourceLocation(String method, String parameter, Locale locale) throws MalformedURLException {
		// e.g. http://api.themoviedb.org/2.1/Movie.search/en/xml/{apikey}/serenity
		return new URL("http", host, "/" + version + "/" + method + "/" + locale.getLanguage() + "/xml/" + apikey + "/" + parameter);
	}
	
	
	protected Document fetchResource(String method, String parameter, Locale locale, final FloodLimit limit) throws IOException, SAXException {
		return getDocument(new CachedPage(getResourceLocation(method, parameter, locale)) {
			
			@Override
			protected Reader openConnection(URL url) throws IOException {
				try {
					if (limit != null) {
						limit.acquirePermit();
					}
					return super.openConnection(url);
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			};
		}.get());
	}
	
	
	public MovieInfo getMovieInfo(Movie movie, Locale locale) throws Exception {
		if (movie.getImdbId() >= 0) {
			return getMovieInfoByIMDbID(movie.getImdbId(), Locale.ENGLISH);
		} else {
			return getMovieInfoByName(movie.getName(), movie.getYear(), Locale.ENGLISH);
		}
	}
	
	
	public MovieInfo getMovieInfoByName(String name, int year, Locale locale) throws Exception {
		for (Movie it : searchMovie(name, locale)) {
			if (name.equalsIgnoreCase(it.getName()) && year == it.getYear()) {
				return getMovieInfo(it, locale);
			}
		}
		
		return null;
	}
	
	
	public MovieInfo getMovieInfoByIMDbID(int imdbid, Locale locale) throws Exception {
		if (imdbid < 0)
			throw new IllegalArgumentException("Illegal IMDb ID: " + imdbid);
		
		// resolve imdbid to tmdbid
		Document dom = fetchResource("Movie.imdbLookup", String.format("tt%07d", imdbid), locale, REQUEST_LIMIT);
		
		String tmdbid = selectString("//movie/id", dom);
		if (tmdbid == null || tmdbid.isEmpty()) {
			throw new IllegalArgumentException("Unable to lookup tmdb entry: " + String.format("tt%07d", imdbid));
		}
		
		// get complete movie info via tmdbid lookup
		dom = fetchResource("Movie.getInfo", tmdbid, locale, REQUEST_LIMIT);
		
		// select info from xml
		Node node = selectNode("//movie", dom);
		
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
		
		List<Person> cast = new ArrayList<Person>();
		for (Node image : selectNodes("//person", node)) {
			Map<PersonProperty, String> personProperties = new EnumMap<PersonProperty, String>(PersonProperty.class);
			for (PersonProperty property : PersonProperty.values()) {
				personProperties.put(property, getAttribute(property.name(), image));
			}
			cast.add(new Person(personProperties));
		}
		
		return new MovieInfo(movieProperties, genres, cast, artwork);
	}
	
	
	public static class MovieInfo implements Serializable {
		
		public static enum MovieProperty {
			translated,
			adult,
			language,
			original_name,
			name,
			type,
			id,
			imdb_id,
			url,
			overview,
			votes,
			rating,
			tagline,
			certification,
			released,
			runtime
		}
		
		
		protected Map<MovieProperty, String> fields;
		protected String[] genres;
		protected Person[] cast;
		protected Artwork[] images;
		
		
		protected MovieInfo() {
			// used by serializer
		}
		
		
		protected MovieInfo(Map<MovieProperty, String> fields, List<String> genres, List<Person> cast, List<Artwork> images) {
			this.fields = new EnumMap<MovieProperty, String>(fields);
			this.genres = genres.toArray(new String[0]);
			this.cast = cast.toArray(new Person[0]);
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
		
		
		public String getOriginalName() {
			return get(MovieProperty.original_name);
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
		
		
		public String getTagline() {
			return get(MovieProperty.tagline);
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
		
		
		public List<Person> getCast() {
			return unmodifiableList(asList(cast));
		}
		
		
		public String getDirector() {
			for (Person person : cast) {
				if (person.isDirector())
					return person.getName();
			}
			return null;
		}
		
		
		public List<String> getActors() {
			List<String> actors = new ArrayList<String>();
			for (Person person : cast) {
				if (person.isActor()) {
					actors.add(person.getName());
				}
			}
			return actors;
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
	
	
	public static class Person implements Serializable {
		
		public static enum PersonProperty {
			name,
			character,
			job,
			thumb,
			department
		}
		
		
		protected Map<PersonProperty, String> fields;
		
		
		protected Person() {
			// used by serializer
		}
		
		
		protected Person(Map<PersonProperty, String> fields) {
			this.fields = new EnumMap<PersonProperty, String>(fields);
		}
		
		
		public String get(Object key) {
			return fields.get(PersonProperty.valueOf(key.toString()));
		}
		
		
		public String get(PersonProperty key) {
			return fields.get(key);
		}
		
		
		public String getName() {
			return get(PersonProperty.name);
		}
		
		
		public String getCharacter() {
			return get(PersonProperty.character);
		}
		
		
		public String getJob() {
			return get(PersonProperty.job);
		}
		
		
		public String getDepartment() {
			return get(PersonProperty.department);
		}
		
		
		public URL getThumb() {
			try {
				return new URL(get(PersonProperty.thumb));
			} catch (Exception e) {
				return null;
			}
		}
		
		
		public boolean isActor() {
			return "Actor".equals(getJob());
		}
		
		
		public boolean isDirector() {
			return "Director".equals(getJob());
		}
		
		
		@Override
		public String toString() {
			return fields.toString();
		}
	}
	
}
