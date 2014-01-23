package net.sourceforge.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.web.WebRequest.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.TMDbClient.MovieInfo.MovieProperty;
import net.sourceforge.filebot.web.TMDbClient.Person.PersonProperty;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class TMDbClient implements MovieIdentificationService {

	private static final String host = "api.themoviedb.org";
	private static final String version = "3";

	private static final FloodLimit SEARCH_LIMIT = new FloodLimit(10, 12, TimeUnit.SECONDS);
	private static final FloodLimit REQUEST_LIMIT = new FloodLimit(20, 12, TimeUnit.SECONDS);

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
		JSONObject response = request("search/movie", singletonMap("query", query), locale, SEARCH_LIMIT);
		List<Movie> result = new ArrayList<Movie>();

		for (JSONObject it : jsonList(response.get("results"))) {
			if (it == null) {
				continue;
			}

			// e.g.
			// {"id":16320,"title":"冲出宁静号","release_date":"2005-09-30","original_title":"Serenity"}
			String title = (String) it.get("title");
			String originalTitle = (String) it.get("original_title");
			if (title == null || title.isEmpty()) {
				title = originalTitle;
			}

			try {
				int id = Float.valueOf(it.get("id").toString()).intValue();
				int year = -1;
				try {
					String release = (String) it.get("release_date");
					year = new Scanner(release).useDelimiter("\\D+").nextInt();
				} catch (Exception e) {
					throw new IllegalArgumentException("Missing data: release date");
				}

				Set<String> alternativeTitles = new LinkedHashSet<String>();
				if (originalTitle != null) {
					alternativeTitles.add(originalTitle);
				}

				try {
					String countryCode = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
					JSONObject titles = request("movie/" + id + "/alternative_titles", null, null, REQUEST_LIMIT);
					for (JSONObject node : jsonList(titles.get("titles"))) {
						if (countryCode.equals(node.get("iso_3166_1"))) {
							alternativeTitles.add((String) node.get("title"));
						}
					}
				} catch (Exception e) {
					Logger.getLogger(TMDbClient.class.getName()).log(Level.WARNING, String.format("Unable to retrieve alternative titles [%s]: %s", title, e.getMessage()));
				}

				// make sure main title is not in the set of alternative titles
				alternativeTitles.remove(title);

				result.add(new Movie(title, alternativeTitles.toArray(new String[0]), year, -1, id));
			} catch (Exception e) {
				// only print 'missing release date' warnings for matching movie titles
				if (query.equalsIgnoreCase(title) || query.equalsIgnoreCase(originalTitle)) {
					Logger.getLogger(TMDbClient.class.getName()).log(Level.WARNING, String.format("Ignore movie [%s]: %s", title, e.getMessage()));
				}
			}
		}
		return result;
	}

	public URI getMoviePageLink(int tmdbid) {
		return URI.create("http://www.themoviedb.org/movie/" + tmdbid);
	}

	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws IOException {
		return getMovieDescriptor(imdbid, locale, true);
	}

	public Movie getMovieDescriptor(int imdbtmdbid, Locale locale, boolean byIMDB) throws IOException {
		String id = byIMDB ? String.format("tt%07d", imdbtmdbid) : String.valueOf(imdbtmdbid);
		try {
			MovieInfo info = getMovieInfo(id, locale, false, false);
			return new Movie(info.getName(), info.getReleased().getYear(), info.getImdbId(), info.getId());
		} catch (FileNotFoundException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Movie not found: " + id);
			return null;
		} catch (NullPointerException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Movie data missing: " + id);
			return null;
		}
	}

	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}

	public MovieInfo getMovieInfo(Movie movie, Locale locale) throws IOException {
		if (movie.getTmdbId() >= 0) {
			return getMovieInfo(String.valueOf(movie.getTmdbId()), locale, true, true);
		} else if (movie.getImdbId() >= 0) {
			return getMovieInfo(String.format("tt%07d", movie.getImdbId()), locale, true, true);
		} else {
			for (Movie result : searchMovie(movie.getName(), locale)) {
				if (movie.getName().equalsIgnoreCase(result.getName()) && movie.getYear() == result.getYear()) {
					return getMovieInfo(String.valueOf(result.getTmdbId()), locale, true, true);
				}
			}
		}
		return null;
	}

	public MovieInfo getMovieInfo(String id, Locale locale, boolean includeAlternativeTitles, boolean includeExtendedInfo) throws IOException {
		JSONObject response = request("movie/" + id, null, locale, REQUEST_LIMIT);

		Map<MovieProperty, String> fields = new EnumMap<MovieProperty, String>(MovieProperty.class);
		for (MovieProperty key : MovieProperty.values()) {
			Object value = response.get(key.name());
			if (value != null) {
				fields.put(key, value.toString());
			}
		}

		try {
			JSONObject collection = (JSONObject) response.get("belongs_to_collection");
			fields.put(MovieProperty.collection, (String) collection.get("name"));
		} catch (Exception e) {
			// ignore
		}

		List<String> genres = new ArrayList<String>();
		for (JSONObject it : jsonList(response.get("genres"))) {
			genres.add((String) it.get("name"));
		}

		List<String> spokenLanguages = new ArrayList<String>();
		for (JSONObject it : jsonList(response.get("spoken_languages"))) {
			spokenLanguages.add((String) it.get("iso_639_1"));
		}

		List<String> alternativeTitles = new ArrayList<String>();
		if (includeAlternativeTitles) {
			String countryCode = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
			JSONObject titles = request("movie/" + fields.get(MovieProperty.id) + "/alternative_titles", null, null, REQUEST_LIMIT);
			for (JSONObject it : jsonList(titles.get("titles"))) {
				if (countryCode.equals(it.get("iso_3166_1"))) {
					alternativeTitles.add((String) it.get("title"));
				}
			}
		}

		if (includeExtendedInfo) {
			String countryCode = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
			JSONObject releases = request("movie/" + fields.get(MovieProperty.id) + "/releases", null, null, REQUEST_LIMIT);
			for (JSONObject it : jsonList(releases.get("countries"))) {
				if (countryCode.equals(it.get("iso_3166_1"))) {
					fields.put(MovieProperty.certification, (String) it.get("certification"));
				}
			}
		}

		List<Person> cast = new ArrayList<Person>();
		if (includeExtendedInfo) {
			JSONObject castResponse = request("movie/" + fields.get(MovieProperty.id) + "/casts", null, null, REQUEST_LIMIT);
			for (String section : new String[] { "cast", "crew" }) {
				for (JSONObject it : jsonList(castResponse.get(section))) {
					Map<PersonProperty, String> person = new EnumMap<PersonProperty, String>(PersonProperty.class);
					for (PersonProperty key : PersonProperty.values()) {
						Object value = it.get(key.name());
						if (value != null) {
							person.put(key, value.toString());
						}
					}
					cast.add(new Person(person));
				}
			}
		}

		List<Trailer> trailers = new ArrayList<Trailer>();
		if (includeExtendedInfo) {
			JSONObject trailerResponse = request("movie/" + fields.get(MovieProperty.id) + "/trailers", null, null, REQUEST_LIMIT);
			for (String section : new String[] { "quicktime", "youtube" }) {
				for (JSONObject it : jsonList(trailerResponse.get(section))) {
					LinkedHashMap<String, String> sources = new LinkedHashMap<String, String>();
					if (it.containsKey("sources")) {
						for (JSONObject s : jsonList(it.get("sources"))) {
							sources.put(s.get("size").toString(), s.get("source").toString());
						}
					} else {
						sources.put(it.get("size").toString(), it.get("source").toString());
					}
					trailers.add(new Trailer(section, it.get("name").toString(), sources));
				}
			}
		}

		return new MovieInfo(fields, alternativeTitles, genres, spokenLanguages, cast, trailers);
	}

	public List<Artwork> getArtwork(String id) throws IOException {
		// http://api.themoviedb.org/3/movie/11/images
		JSONObject config = request("configuration", null, null, REQUEST_LIMIT);
		String baseUrl = (String) ((JSONObject) config.get("images")).get("base_url");

		JSONObject images = request("movie/" + id + "/images", null, null, REQUEST_LIMIT);
		List<Artwork> artwork = new ArrayList<Artwork>();

		for (String section : new String[] { "backdrops", "posters" }) {
			for (JSONObject it : jsonList(images.get(section))) {
				try {
					String url = baseUrl + "original" + (String) it.get("file_path");
					int width = Float.valueOf(it.get("width").toString()).intValue();
					int height = Float.valueOf(it.get("height").toString()).intValue();
					String lang = (String) it.get("iso_639_1");
					artwork.add(new Artwork(section, new URL(url), width, height, lang));
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid artwork: " + it, e);
				}
			}
		}

		return artwork;
	}

	public JSONObject request(String resource, Map<String, String> parameters, Locale locale, final FloodLimit limit) throws IOException {
		// default parameters
		LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
		if (parameters != null) {
			data.putAll(parameters);
		}
		if (locale != null && !locale.getLanguage().isEmpty()) {
			data.put("language", locale.getLanguage());
		}
		data.put("api_key", apikey);

		URL url = new URL("http", host, "/" + version + "/" + resource + "?" + encodeParameters(data, true));

		CachedResource<String> json = new ETagCachedResource<String>(url.toString(), String.class) {

			@Override
			public String process(ByteBuffer data) throws Exception {
				return Charset.forName("UTF-8").decode(data).toString();
			}

			@Override
			protected ByteBuffer fetchData(URL url, long lastModified) throws IOException {
				try {
					if (limit != null) {
						limit.acquirePermit();
					}
					return super.fetchData(url, lastModified);
				} catch (FileNotFoundException e) {
					return ByteBuffer.allocate(0);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};

		JSONObject object = (JSONObject) JSONValue.parse(json.get());
		if (object == null || object.isEmpty()) {
			throw new FileNotFoundException("Resource not found: " + url);
		}
		return object;
	}

	protected List<JSONObject> jsonList(final Object array) {
		return new AbstractList<JSONObject>() {

			@Override
			public JSONObject get(int index) {
				return (JSONObject) ((JSONArray) array).get(index);
			}

			@Override
			public int size() {
				return ((JSONArray) array).size();
			}
		};
	}

	public static class MovieInfo implements Serializable {

		public static enum MovieProperty {
			adult, backdrop_path, budget, homepage, id, imdb_id, original_title, overview, popularity, poster_path, release_date, revenue, runtime, tagline, title, vote_average, vote_count, certification, collection
		}

		protected Map<MovieProperty, String> fields;

		protected String[] alternativeTitles;
		protected String[] genres;
		protected String[] spokenLanguages;

		protected Person[] people;
		protected Trailer[] trailers;

		protected MovieInfo() {
			// used by serializer
		}

		protected MovieInfo(Map<MovieProperty, String> fields, List<String> alternativeTitles, List<String> genres, List<String> spokenLanguages, List<Person> people, List<Trailer> trailers) {
			this.fields = new EnumMap<MovieProperty, String>(fields);
			this.alternativeTitles = alternativeTitles.toArray(new String[0]);
			this.genres = genres.toArray(new String[0]);
			this.spokenLanguages = spokenLanguages.toArray(new String[0]);
			this.people = people.toArray(new Person[0]);
			this.trailers = trailers.toArray(new Trailer[0]);
		}

		public String get(Object key) {
			return fields.get(MovieProperty.valueOf(key.toString()));
		}

		public String get(MovieProperty key) {
			return fields.get(key);
		}

		public boolean isAdult() {
			return Boolean.valueOf(get(MovieProperty.adult));
		}

		public List<Locale> getSpokenLanguages() {
			try {
				List<Locale> locales = new ArrayList<Locale>();
				for (String it : spokenLanguages) {
					locales.add(new Locale(it));
				}
				return locales;
			} catch (Exception e) {
				return null;
			}
		}

		public String getOriginalName() {
			return get(MovieProperty.original_title);
		}

		public String getName() {
			return get(MovieProperty.title);
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

		public URL getHomepage() {
			try {
				return new URL(get(MovieProperty.homepage));
			} catch (Exception e) {
				return null;
			}
		}

		public String getOverview() {
			return get(MovieProperty.overview);
		}

		public Integer getVotes() {
			try {
				return new Integer(get(MovieProperty.vote_count));
			} catch (Exception e) {
				return null;
			}
		}

		public Double getRating() {
			try {
				return new Double(get(MovieProperty.vote_average));
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

		public String getCollection() {
			// e.g. Star Wars Collection
			return get(MovieProperty.collection);
		}

		public Date getReleased() {
			// e.g. 2005-09-30
			try {
				return Date.parse(get(MovieProperty.release_date), "yyyy-MM-dd");
			} catch (Exception e) {
				return null;
			}
		}

		public String getRuntime() {
			return get(MovieProperty.runtime);
		}

		public List<String> getGenres() {
			return unmodifiableList(asList(genres));
		}

		public List<Person> getPeople() {
			return unmodifiableList(asList(people));
		}

		public List<Person> getCast() {
			List<Person> actors = new ArrayList<Person>();
			for (Person person : people) {
				if (person.isActor()) {
					actors.add(person);
				}
			}
			return actors;
		}

		public String getDirector() {
			for (Person person : people) {
				if (person.isDirector())
					return person.getName();
			}
			return null;
		}

		public String getWriter() {
			for (Person person : people) {
				if (person.isWriter())
					return person.getName();
			}
			return null;
		}

		public List<String> getActors() {
			List<String> actors = new ArrayList<String>();
			for (Person actor : getCast()) {
				actors.add(actor.getName());
			}
			return actors;
		}

		public URL getPoster() {
			try {
				return new URL(get(MovieProperty.poster_path));
			} catch (Exception e) {
				return null;
			}
		}

		public List<Trailer> getTrailers() {
			return unmodifiableList(asList(trailers));
		}

		public List<String> getAlternativeTitles() {
			return unmodifiableList(asList(alternativeTitles));
		}

		@Override
		public String toString() {
			return fields.toString();
		}
	}

	public static class Person implements Serializable {

		public static enum PersonProperty {
			name, character, job
		}

		protected Map<PersonProperty, String> fields;

		protected Person() {
			// used by serializer
		}

		public Person(Map<PersonProperty, String> fields) {
			this.fields = new EnumMap<PersonProperty, String>(fields);
		}

		public Person(String name, String character, String job) {
			fields = new EnumMap<PersonProperty, String>(PersonProperty.class);
			fields.put(PersonProperty.name, name);
			fields.put(PersonProperty.character, character);
			fields.put(PersonProperty.job, job);
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

		public boolean isActor() {
			return getJob() == null;
		}

		public boolean isDirector() {
			return "Director".equals(getJob());
		}

		public boolean isWriter() {
			return "Writer".equals(getJob());
		}

		@Override
		public String toString() {
			return fields.toString();
		}
	}

	public static class Artwork {

		private String category;
		private String language;

		private int width;
		private int height;

		private URL url;

		public Artwork(String category, URL url, int width, int height, String language) {
			this.category = category;
			this.url = url;
			this.width = width;
			this.height = height;
			this.language = language;
		}

		public String getCategory() {
			return category;
		}

		public String getLanguage() {
			return language;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public URL getUrl() {
			return url;
		}

		@Override
		public String toString() {
			return String.format("{category: %s, width: %s, height: %s, language: %s, url: %s}", category, width, height, language, url);
		}
	}

	public static class Trailer {

		private String type;
		private String name;
		private Map<String, String> sources;

		public Trailer(String type, String name, Map<String, String> sources) {
			this.type = type;
			this.name = name;
			this.sources = sources;
		}

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public Map<String, String> getSources() {
			return sources;
		}

		public String getSource(String size) {
			return sources.containsKey(size) ? sources.get(size) : sources.values().iterator().next();
		}

		@Override
		public String toString() {
			return String.format("%s %s (%s)", name, sources.keySet(), type);
		}

	}

}
