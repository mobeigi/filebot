package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.CachedResource2.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.web.WebRequest.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.Language;
import net.filebot.ResourceManager;
import net.filebot.web.TMDbClient.MovieInfo.MovieProperty;
import net.filebot.web.TMDbClient.Person.PersonProperty;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class TMDbClient implements MovieIdentificationService {

	private static final String host = "api.themoviedb.org";
	private static final String version = "3";

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
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		// query by name with year filter if possible
		Matcher nameYear = Pattern.compile("(.+)\\b\\(?(19\\d{2}|20\\d{2})\\)?$").matcher(query.trim());
		if (nameYear.matches()) {
			return searchMovie(nameYear.group(1).trim(), Integer.parseInt(nameYear.group(2)), locale, false);
		} else {
			return searchMovie(query, -1, locale, false);
		}
	}

	public List<Movie> searchMovie(String movieName, int movieYear, Locale locale, boolean extendedInfo) throws Exception {
		// ignore queries that are too short to yield good results
		if (movieName.length() < 3 && !(movieName.length() >= 1 && movieYear > 0)) {
			return emptyList();
		}

		Map<String, Object> param = new LinkedHashMap<String, Object>(2);
		param.put("query", movieName);
		if (movieYear > 0) {
			param.put("year", movieYear);
		}

		JSONObject response = request("search/movie", param, locale, SEARCH_LIMIT);
		List<Movie> result = new ArrayList<Movie>();

		for (JSONObject it : jsonList(response.get("results"))) {
			if (it == null) {
				continue;
			}

			// e.g.
			// {"id":16320,"title":"冲出宁静号","release_date":"2005-09-30","original_title":"Serenity"}
			int id = -1, year = -1;
			String title = (String) it.get("title");
			String originalTitle = (String) it.get("original_title");
			if (title == null || title.isEmpty()) {
				title = originalTitle;
			}

			try {
				id = Float.valueOf(it.get("id").toString()).intValue();
				try {
					String release = (String) it.get("release_date");
					year = matchInteger(release);
				} catch (Exception e) {
					throw new IllegalArgumentException("Missing data: release date");
				}

				Set<String> alternativeTitles = new LinkedHashSet<String>();
				if (originalTitle != null) {
					alternativeTitles.add(originalTitle);
				}

				if (extendedInfo) {
					try {
						Set<String> internationalTitles = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
						JSONObject titles = request("movie/" + id + "/alternative_titles", null, null, REQUEST_LIMIT);
						for (JSONObject node : jsonList(titles.get("titles"))) {
							String t = (String) node.get("title");
							if (t != null && t.length() >= 3) {
								internationalTitles.add(t);
							}
						}
						alternativeTitles.addAll(internationalTitles);
					} catch (Exception e) {
						Logger.getLogger(TMDbClient.class.getName()).log(Level.WARNING, String.format("Unable to retrieve alternative titles [%s]: %s", title, e.getMessage()));
					}
				}

				// make sure main title is not in the set of alternative titles
				alternativeTitles.remove(title);

				result.add(new Movie(title, alternativeTitles.toArray(new String[0]), year, -1, id, locale));
			} catch (Exception e) {
				// only print 'missing release date' warnings for matching movie titles
				if (movieName.equalsIgnoreCase(title) || movieName.equalsIgnoreCase(originalTitle)) {
					Logger.getLogger(TMDbClient.class.getName()).log(Level.WARNING, String.format("Ignore movie metadata: %s [%d]: %s", title, id, e.getMessage()));
				}
			}
		}
		return result;
	}

	public URI getMoviePageLink(int tmdbid) {
		return URI.create("http://www.themoviedb.org/movie/" + tmdbid);
	}

	@Override
	public Movie getMovieDescriptor(Movie id, Locale locale) throws Exception {
		if (id.getTmdbId() > 0 || id.getImdbId() > 0) {
			MovieInfo info = getMovieInfo(id, locale, false);
			if (info != null) {
				String name = info.getName();
				String[] aliasNames = info.getOriginalName() == null || info.getOriginalName().isEmpty() || info.getOriginalName().equals(name) ? new String[0] : new String[] { info.getOriginalName() };
				int year = info.getReleased() != null ? info.getReleased().getYear() : id.getYear();
				int tmdbid = info.getId();
				int imdbid = info.getImdbId() != null ? info.getImdbId() : -1;
				return new Movie(name, aliasNames, year, imdbid, tmdbid, locale);
			}
		}
		return null;
	}

	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}

	public MovieInfo getMovieInfo(Movie movie, Locale locale, boolean extendedInfo) throws Exception {
		try {
			if (movie.getTmdbId() > 0) {
				return getMovieInfo(String.valueOf(movie.getTmdbId()), locale, extendedInfo);
			} else if (movie.getImdbId() > 0) {
				return getMovieInfo(String.format("tt%07d", movie.getImdbId()), locale, extendedInfo);
			}
		} catch (FileNotFoundException | NullPointerException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, String.format("Movie data not found: %s [%d / %d]", movie, movie.getTmdbId(), movie.getImdbId()));
		}
		return null;
	}

	public MovieInfo getMovieInfo(String id, Locale locale, boolean extendedInfo) throws Exception {
		JSONObject response = request("movie/" + id, extendedInfo ? singletonMap("append_to_response", "alternative_titles,releases,casts,trailers") : null, locale, REQUEST_LIMIT);

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
			// movie doesn't belong to any collection
		}

		List<String> genres = new ArrayList<String>();
		try {
			for (JSONObject it : jsonList(response.get("genres"))) {
				String name = (String) it.get("name");
				if (name != null && name.length() > 0) {
					genres.add(name);
				}
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal genres data: " + response);
		}

		List<String> spokenLanguages = new ArrayList<String>();
		try {
			for (JSONObject it : jsonList(response.get("spoken_languages"))) {
				spokenLanguages.add((String) it.get("iso_639_1"));
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal spoken_languages data: " + response);
		}

		List<String> productionCountries = new ArrayList<String>();
		try {
			for (JSONObject it : jsonList(response.get("production_countries"))) {
				productionCountries.add((String) it.get("iso_3166_1"));
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal production_countries data: " + response);
		}

		List<String> productionCompanies = new ArrayList<String>();
		try {
			for (JSONObject it : jsonList(response.get("production_companies"))) {
				productionCompanies.add((String) it.get("name"));
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal production_companies data: " + response);
		}

		List<String> alternativeTitles = new ArrayList<String>();
		try {
			JSONObject titles = (JSONObject) response.get("alternative_titles");
			if (titles != null) {
				for (JSONObject it : jsonList(titles.get("titles"))) {
					alternativeTitles.add((String) it.get("title"));
				}
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal alternative_titles data: " + response);
		}

		Map<String, String> certifications = new HashMap<String, String>();
		try {
			String countryCode = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
			JSONObject releases = (JSONObject) response.get("releases");
			if (releases != null) {
				for (JSONObject it : jsonList(releases.get("countries"))) {
					String certificationCountry = (String) it.get("iso_3166_1");
					String certification = (String) it.get("certification");
					if (certification != null && certificationCountry != null && certification.length() > 0 && certificationCountry.length() > 0) {
						// add country specific certification code
						if (countryCode.equals(certificationCountry)) {
							fields.put(MovieProperty.certification, certification);
						}
						// collect all certification codes just in case
						certifications.put(certificationCountry, certification);
					}
				}
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal releases data: " + response);
		}

		List<Person> cast = new ArrayList<Person>();
		try {
			JSONObject castResponse = (JSONObject) response.get("casts");
			if (castResponse != null) {
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
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal casts data: " + response);
		}

		List<Trailer> trailers = new ArrayList<Trailer>();
		try {
			JSONObject trailerResponse = (JSONObject) response.get("trailers");
			if (trailerResponse != null) {
				for (String section : new String[] { "quicktime", "youtube" }) {
					for (JSONObject it : jsonList(trailerResponse.get(section))) {
						Map<String, String> sources = new LinkedHashMap<String, String>();
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
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal trailers data: " + response);
		}

		return new MovieInfo(fields, alternativeTitles, genres, certifications, spokenLanguages, productionCountries, productionCompanies, cast, trailers);
	}

	public List<Artwork> getArtwork(String id) throws Exception {
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

	public JSONObject request(String resource, Map<String, Object> parameters, Locale locale, final FloodLimit limit) throws Exception {
		// default parameters
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		if (parameters != null) {
			data.putAll(parameters);
		}

		if (locale != null && locale.getLanguage().length() > 0) {
			String code = locale.getLanguage();

			// require 2-letter language code
			if (code.length() != 2) {
				Language lang = Language.getLanguage(locale);
				if (lang != null) {
					code = lang.getISO2();
				}
			}
			data.put("language", code);
		}
		data.put("api_key", apikey);

		String key = resource + '?' + encodeParameters(data, true);

		Cache etagStorage = Cache.getCache("etag", CacheType.Monthly);
		Cache cache = Cache.getCache(getName(), CacheType.Monthly);
		String json = cache.text(key, s -> getResource(s), Cache.ONE_WEEK, withPermit(fetchIfNoneMatch(etagStorage), r -> REQUEST_LIMIT.acquirePermit() != null)).get();

		JSONObject object = (JSONObject) JSONValue.parse(json);
		if (object == null || object.isEmpty()) {
			throw new FileNotFoundException("Resource not found: " + getResource(key));
		}
		return object;
	}

	public URL getResource(String file) throws Exception {
		return new URL("http", host, "/" + version + "/" + file);
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
		protected String[] productionCountries;
		protected String[] productionCompanies;
		protected Map<String, String> certifications;

		protected Person[] people;
		protected Trailer[] trailers;

		protected MovieInfo() {
			// used by serializer
		}

		protected MovieInfo(Map<MovieProperty, String> fields, List<String> alternativeTitles, List<String> genres, Map<String, String> certifications, List<String> spokenLanguages, List<String> productionCountries, List<String> productionCompanies, List<Person> people, List<Trailer> trailers) {
			this.fields = new EnumMap<MovieProperty, String>(fields);
			this.alternativeTitles = alternativeTitles.toArray(new String[0]);
			this.genres = genres.toArray(new String[0]);
			this.certifications = new HashMap<String, String>(certifications);
			this.spokenLanguages = spokenLanguages.toArray(new String[0]);
			this.productionCountries = productionCountries.toArray(new String[0]);
			this.productionCompanies = productionCompanies.toArray(new String[0]);
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

		public List<String> getProductionCountries() {
			return unmodifiableList(asList(productionCountries));
		}

		public List<String> getProductionCompanies() {
			return unmodifiableList(asList(productionCompanies));
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

		public Map<String, String> getCertifications() {
			// e.g. ['US': PG-13]
			return unmodifiableMap(certifications);
		}

		public String getCollection() {
			// e.g. Star Wars Collection
			return get(MovieProperty.collection);
		}

		public SimpleDate getReleased() {
			// e.g. 2005-09-30
			try {
				return SimpleDate.parse(get(MovieProperty.release_date));
			} catch (Exception e) {
				return null;
			}
		}

		public String getRuntime() {
			return get(MovieProperty.runtime);
		}

		public Long getBudget() {
			try {
				return new Long(get(MovieProperty.budget));
			} catch (Exception e) {
				return null;
			}
		}

		public Long getRevenue() {
			try {
				return new Long(get(MovieProperty.revenue));
			} catch (Exception e) {
				return null;
			}
		}

		public Double getPopularity() {
			try {
				return new Double(get(MovieProperty.popularity));
			} catch (Exception e) {
				return null;
			}
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
