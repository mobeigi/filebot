package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.CachedResource.*;
import static net.filebot.Logging.*;
import static net.filebot.similarity.Normalization.*;
import static net.filebot.util.JsonUtilities.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.web.WebRequest.*;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.Language;
import net.filebot.ResourceManager;

public class TMDbClient implements MovieIdentificationService, ArtworkProvider {

	private static final String host = "api.themoviedb.org";
	private static final String version = "3";

	protected static final FloodLimit SEARCH_LIMIT = new FloodLimit(10, 10, TimeUnit.SECONDS);
	protected static final FloodLimit REQUEST_LIMIT = new FloodLimit(20, 10, TimeUnit.SECONDS);

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

	protected Matcher getNameYearMatcher(String query) {
		return Pattern.compile("(.+)\\b[(]?((?:19|20)\\d{2})[)]?$").matcher(query.trim());
	}

	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		// query by name with year filter if possible
		Matcher nameYear = getNameYearMatcher(query);
		if (nameYear.matches()) {
			return searchMovie(nameYear.group(1).trim(), Integer.parseInt(nameYear.group(2)), locale, false);
		} else {
			return searchMovie(query.trim(), -1, locale, false);
		}
	}

	public List<Movie> searchMovie(String movieName, int movieYear, Locale locale, boolean extendedInfo) throws Exception {
		// ignore queries that are too short to yield good results
		if (movieName.length() < 3 && !(movieName.length() >= 1 && movieYear > 0)) {
			return emptyList();
		}

		Map<String, Object> query = new LinkedHashMap<String, Object>(2);
		query.put("query", movieName);
		if (movieYear > 0) {
			query.put("year", movieYear);
		}
		Object response = request("search/movie", query, locale, SEARCH_LIMIT);

		// e.g. {"id":16320,"title":"冲出宁静号","release_date":"2005-09-30","original_title":"Serenity"}
		return streamJsonObjects(response, "results").map(it -> {
			int id = -1, year = -1;
			try {
				id = getDecimal(it, "id").intValue();
				year = matchInteger(getString(it, "release_date")); // release date is often missing
			} catch (Exception e) {
				debug.fine(format("Missing data: release_date => %s", it));
				return null;
			}

			String title = getString(it, "title");
			String originalTitle = getString(it, "original_title");
			if (title == null) {
				title = originalTitle;
			}

			String[] alternativeTitles = getAlternativeTitles("movie/" + id, "titles", title, originalTitle, extendedInfo);

			return new Movie(title, alternativeTitles, year, -1, id, locale);
		}).filter(Objects::nonNull).collect(toList());
	}

	protected String[] getAlternativeTitles(String path, String key, String title, String originalTitle, boolean extendedInfo) {
		Set<String> alternativeTitles = new LinkedHashSet<String>();
		if (originalTitle != null) {
			alternativeTitles.add(originalTitle);
		}

		if (extendedInfo) {
			try {
				Object response = request(path + "/alternative_titles", emptyMap(), Locale.ENGLISH, REQUEST_LIMIT);
				streamJsonObjects(response, key).map(n -> {
					return getString(n, "title");
				}).filter(Objects::nonNull).filter(n -> n.length() >= 2).forEach(alternativeTitles::add);
			} catch (Exception e) {
				debug.warning(format("Failed to fetch alternative titles for %s => %s", path, e));
			}
		}

		// make sure main title is not in the set of alternative titles
		alternativeTitles.remove(title);

		return alternativeTitles.toArray(new String[0]);
	}

	public URI getMoviePageLink(int tmdbid) {
		return URI.create("https://www.themoviedb.org/movie/" + tmdbid);
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
				int imdbid = info.getImdbId() != null ? info.getImdbId() : 0;
				return new Movie(name, aliasNames, year, imdbid, tmdbid, locale);
			}
		}
		return null;
	}

	public MovieInfo getMovieInfo(Movie movie, Locale locale, boolean extendedInfo) throws Exception {
		try {
			if (movie.getTmdbId() > 0) {
				return getMovieInfo(String.valueOf(movie.getTmdbId()), locale, extendedInfo);
			} else if (movie.getImdbId() > 0) {
				return getMovieInfo(String.format("tt%07d", movie.getImdbId()), locale, extendedInfo);
			}
		} catch (FileNotFoundException | NullPointerException e) {
			debug.log(Level.WARNING, String.format("Movie data not found: %s [%d / %d]", movie, movie.getTmdbId(), movie.getImdbId()));
		}
		return null;
	}

	public MovieInfo getMovieInfo(String id, Locale locale, boolean extendedInfo) throws Exception {
		Object response = request("movie/" + id, extendedInfo ? singletonMap("append_to_response", "alternative_titles,releases,casts,trailers") : emptyMap(), locale, REQUEST_LIMIT);

		Map<MovieProperty, String> fields = getEnumMap(response, MovieProperty.class);

		try {
			Map<?, ?> collection = getMap(response, "belongs_to_collection");
			fields.put(MovieProperty.collection, getString(collection, "name"));
		} catch (Exception e) {
			// movie does not belong to any collection
			debug.warning(format("Bad data: belongs_to_collection => %s", response));
		}

		List<String> genres = new ArrayList<String>();
		try {
			streamJsonObjects(response, "genres").map(it -> getString(it, "name")).filter(Objects::nonNull).forEach(genres::add);
		} catch (Exception e) {
			debug.warning(format("Bad data: genres => %s", response));
		}

		List<String> spokenLanguages = new ArrayList<String>();
		try {
			streamJsonObjects(response, "spoken_languages").map(it -> getString(it, "iso_639_1")).filter(Objects::nonNull).forEach(spokenLanguages::add);
		} catch (Exception e) {
			debug.warning(format("Bad data: spoken_languages => %s", response));
		}

		List<String> productionCountries = new ArrayList<String>();
		try {
			streamJsonObjects(response, "production_countries").map(it -> getString(it, "iso_3166_1")).filter(Objects::nonNull).forEach(productionCountries::add);
		} catch (Exception e) {
			debug.warning(format("Bad data: production_countries => %s", response));
		}

		List<String> productionCompanies = new ArrayList<String>();
		try {
			streamJsonObjects(response, "production_companies").map(it -> getString(it, "name")).filter(Objects::nonNull).forEach(productionCompanies::add);
		} catch (Exception e) {
			debug.warning(format("Bad data: production_companies => %s", response));
		}

		List<String> alternativeTitles = new ArrayList<String>();
		try {
			streamJsonObjects(getMap(response, "alternative_titles"), "titles").map(it -> getString(it, "title")).filter(Objects::nonNull).forEach(alternativeTitles::add);
		} catch (Exception e) {
			debug.warning(format("Bad data: alternative_titles => %s", response));
		}

		Map<String, String> certifications = new LinkedHashMap<String, String>();
		try {
			String countryCode = locale.getCountry().isEmpty() ? "US" : locale.getCountry();

			streamJsonObjects(getMap(response, "releases"), "countries").forEach(it -> {
				String certificationCountry = getString(it, "iso_3166_1");
				String certification = getString(it, "certification");

				if (certification != null && certificationCountry != null) {
					// add country specific certification code
					if (countryCode.equals(certificationCountry)) {
						fields.put(MovieProperty.certification, certification);
					}

					// collect all certification codes just in case
					certifications.put(certificationCountry, certification);
				}
			});
		} catch (Exception e) {
			debug.warning(format("Bad data: certification => %s", response));
		}

		List<Person> cast = new ArrayList<Person>();
		try {
			// { "cast_id":20, "character":"Gandalf", "credit_id":"52fe4a87c3a368484e158bb7", "id":1327, "name":"Ian McKellen", "order":1, "profile_path":"/c51mP46oPgAgFf7bFWVHlScZynM.jpg" }
			Function<String, String> normalize = s -> replaceSpace(s, " ").trim(); // user data may not be well-formed

			Stream.of("cast", "crew").flatMap(section -> streamJsonObjects(getMap(response, "casts"), section)).map(it -> {
				String name = getStringValue(it, "name", normalize);
				String character = getStringValue(it, "character", normalize);
				String job = getStringValue(it, "job", normalize);
				String department = getStringValue(it, "department", normalize);
				Integer order = getInteger(it, "order");
				URL image = getStringValue(it, "profile_path", this::resolveImage);

				return new Person(name, character, job, department, order, image);
			}).sorted(Person.CREDIT_ORDER).forEach(cast::add);
		} catch (Exception e) {
			debug.warning(format("Bad data: casts => %s", response));
		}

		List<Trailer> trailers = new ArrayList<Trailer>();
		try {
			Stream.of("quicktime", "youtube").forEach(section -> {
				streamJsonObjects(getMap(response, "trailers"), section).map(it -> {
					Map<String, String> sources = new LinkedHashMap<String, String>();
					Stream.concat(Stream.of(it), streamJsonObjects(it, "sources")).forEach(source -> {
						String size = getString(source, "size");
						if (size != null) {
							sources.put(size, getString(source, "source"));
						}
					});
					return new Trailer(section, getString(it, "name"), sources);
				}).forEach(trailers::add);
			});
		} catch (Exception e) {
			debug.warning(format("Bad data: trailers => %s", response));
		}

		return new MovieInfo(fields, alternativeTitles, genres, certifications, spokenLanguages, productionCountries, productionCompanies, cast, trailers);
	}

	@Override
	public List<Artwork> getArtwork(int id, String category, Locale locale) throws Exception {
		Object images = request("movie/" + id + "/images", emptyMap(), Locale.ROOT, REQUEST_LIMIT);

		return streamJsonObjects(images, category).map(it -> {
			URL image = getStringValue(it, "file_path", this::resolveImage);
			String width = getString(it, "width");
			String height = getString(it, "height");
			Locale language = getStringValue(it, "iso_639_1", Locale::new);

			return new Artwork(Stream.of(category, String.join("x", width, height)), image, language, null);
		}).sorted(Artwork.RATING_ORDER).collect(toList());
	}

	protected Object getConfiguration() throws Exception {
		return request("configuration", emptyMap(), Locale.ROOT, REQUEST_LIMIT);
	}

	protected URL resolveImage(String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}

		try {
			String mirror = (String) Cache.getCache(getName(), CacheType.Monthly).computeIfAbsent("configuration.base_url", it -> {
				return getString(getMap(getConfiguration(), "images"), "base_url");
			});
			return new URL(mirror + "original" + path);
		} catch (Exception e) {
			throw new IllegalArgumentException(path, e);
		}
	}

	public Map<String, List<String>> getAlternativeTitles(int id) throws Exception {
		Object titles = request("movie/" + id + "/alternative_titles", emptyMap(), Locale.ROOT, REQUEST_LIMIT);

		return streamJsonObjects(titles, "titles").collect(groupingBy(it -> {
			return getString(it, "iso_3166_1");
		}, mapping(it -> {
			return getString(it, "title");
		}, toList())));
	}

	public List<Movie> discover(LocalDate startDate, LocalDate endDate, Locale locale) throws Exception {
		Map<String, Object> parameters = new LinkedHashMap<String, Object>(3);
		parameters.put("primary_release_date.gte", startDate);
		parameters.put("primary_release_date.lte", endDate);
		parameters.put("sort_by", "popularity.desc");
		return discover(parameters, locale);
	}

	public List<Movie> discover(int year, Locale locale) throws Exception {
		Map<String, Object> parameters = new LinkedHashMap<String, Object>(2);
		parameters.put("primary_release_year", year);
		parameters.put("sort_by", "vote_count.desc");
		return discover(parameters, locale);
	}

	public List<Movie> discover(Map<String, Object> parameters, Locale locale) throws Exception {
		Object json = request("discover/movie", parameters, locale, REQUEST_LIMIT);

		return streamJsonObjects(json, "results").map(it -> {
			String title = getString(it, "title");
			int year = getStringValue(it, "release_date", SimpleDate::parse).getYear();
			int id = getInteger(it, "id");
			return new Movie(title, year, 0, id);
		}).collect(toList());
	}

	protected Object request(String resource, Map<String, Object> parameters, Locale locale, FloodLimit limit) throws Exception {
		// default parameters
		String key = parameters.isEmpty() ? resource : resource + '?' + encodeParameters(parameters, true);
		String cacheName = locale.getLanguage().isEmpty() ? getName() : getName() + "_" + locale;

		Cache cache = Cache.getCache(cacheName, CacheType.Monthly);
		Object json = cache.json(key, k -> getResource(k, locale)).fetch(withPermit(fetchIfNoneMatch(url -> key, cache), r -> limit.acquirePermit())).expire(Cache.ONE_WEEK).get();

		if (asMap(json).isEmpty()) {
			throw new FileNotFoundException(String.format("Resource is empty: %s => %s", json, getResource(key, locale)));
		}
		return json;
	}

	protected URL getResource(String path, Locale locale) throws Exception {
		StringBuilder file = new StringBuilder();
		file.append('/').append(version);
		file.append('/').append(path);
		file.append(path.lastIndexOf('?') < 0 ? '?' : '&');

		if (locale.getLanguage().length() > 0) {
			file.append("language=").append(getLanguageCode(locale)).append('&');
		}
		file.append("api_key=").append(apikey);

		return new URL("https", host, file.toString());
	}

	protected String getLanguageCode(Locale locale) {
		// require 2-letter language code
		String language = locale.getLanguage();
		if (language.length() == 2) {
			return language;
		}

		Language lang = Language.getLanguage(locale);
		if (lang != null) {
			return lang.getISO2();
		}

		throw new IllegalArgumentException("Illegal language code: " + language);
	}

	public static enum MovieProperty {
		adult, backdrop_path, budget, homepage, id, imdb_id, original_title, overview, popularity, poster_path, release_date, revenue, runtime, tagline, title, vote_average, vote_count, certification, collection
	}

	public static class MovieInfo implements Serializable {

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
			this.certifications = new LinkedHashMap<String, String>(certifications);
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

	public static class Trailer implements Serializable {

		protected String type;
		protected String name;
		protected Map<String, String> sources;

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
