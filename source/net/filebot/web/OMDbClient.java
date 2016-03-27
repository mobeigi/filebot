package net.filebot.web;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.CachedResource.*;
import static net.filebot.Logging.*;
import static net.filebot.util.JsonUtilities.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.web.WebRequest.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.ResourceManager;
import net.filebot.web.TMDbClient.MovieInfo;
import net.filebot.web.TMDbClient.MovieInfo.MovieProperty;
import net.filebot.web.TMDbClient.Person;

public class OMDbClient implements MovieIdentificationService {

	private static final FloodLimit REQUEST_LIMIT = new FloodLimit(20, 10, TimeUnit.SECONDS);

	@Override
	public String getName() {
		return "OMDb";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.omdb");
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
		// query by name with year filter if possible
		Matcher nameYear = Pattern.compile("(.+)\\b(19\\d{2}|20\\d{2})$").matcher(query);
		if (nameYear.matches()) {
			return searchMovie(nameYear.group(1).trim(), Integer.parseInt(nameYear.group(2)));
		} else {
			return searchMovie(query, -1);
		}
	}

	public List<Movie> searchMovie(String movieName, int movieYear) throws Exception {
		Map<String, Object> param = new LinkedHashMap<String, Object>(2);
		param.put("s", movieName);
		if (movieYear > 0) {
			param.put("y", movieYear);
		}

		Object response = request(param);

		List<Movie> result = new ArrayList<Movie>();
		for (Object it : getArray(response, "Search")) {
			Map<String, String> info = getInfoMap(it);
			if ("movie".equals(info.get("Type"))) {
				result.add(getMovie(info));
			}
		}
		return result;
	}

	@Override
	public Movie getMovieDescriptor(Movie id, Locale locale) throws Exception {
		if (id.getImdbId() <= 0) {
			throw new IllegalArgumentException("Illegal ID: " + id.getImdbId());
		}

		// request full movie info for given id
		return getMovie(getMovieInfo(id.getImdbId(), null, null, false));
	}

	public Map<String, String> getInfoMap(Object node) {
		Map<String, String> info = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		if (node instanceof Map) {
			for (Entry<?, ?> it : ((Map<?, ?>) node).entrySet()) {
				if (it.getKey() != null && it.getValue() != null) {
					info.put(it.getKey().toString().trim(), it.getValue().toString().trim());
				}
			}
		}
		return info;
	}

	public Movie getMovie(Map<String, String> info) {
		try {
			String name = info.get("Title");
			int year = matchInteger(info.get("Year"));
			int imdbid = Integer.parseInt(info.get("imdbID").replace("tt", ""));

			if (name.length() <= 0 || year <= 1900 || imdbid <= 0)
				throw new IllegalArgumentException();

			return new Movie(name, year, imdbid, -1);
		} catch (Exception e) {
			throw new IllegalArgumentException("Illegal fields: " + info);
		}
	}

	public Object request(Map<String, Object> parameters) throws Exception {
		Cache cache = Cache.getCache(getName(), CacheType.Weekly);
		String key = '?' + encodeParameters(parameters, true);

		return cache.json(key, s -> getResource(s)).fetch(withPermit(fetchIfModified(), r -> REQUEST_LIMIT.acquirePermit())).expire(Cache.ONE_WEEK).get();
	}

	public URL getResource(String file) throws Exception {
		return new URL("http://www.omdbapi.com/" + file);
	}

	public Map<String, String> getMovieInfo(Integer i, String t, String y, boolean tomatoes) throws Exception {
		// e.g. http://www.imdbapi.com/?i=tt0379786&r=xml&tomatoes=true
		Map<String, Object> param = new LinkedHashMap<String, Object>(2);
		if (i != null) {
			param.put("i", String.format("tt%07d", i));
		}
		if (t != null) {
			param.put("t", t);
		}
		if (y != null) {
			param.put("y", y);
		}
		param.put("tomatoes", String.valueOf(tomatoes));

		return getInfoMap(request(param));
	}

	public MovieInfo getMovieInfo(Movie movie) throws Exception {
		Map<String, String> data = movie.getImdbId() > 0 ? getMovieInfo(movie.getImdbId(), null, null, false) : getMovieInfo(null, movie.getName(), String.valueOf(movie.getYear()), false);

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
		SimpleDate release = parsePartialDate(data.get("released"), "d MMM yyyy");
		if (release == null) {
			release = parsePartialDate(data.get("released"), "yyyy");
		}
		if (release != null) {
			fields.put(MovieProperty.release_date, release.toString());
		}

		// convert lists
		Pattern delim = Pattern.compile(",");
		List<String> genres = split(delim, data.get("genre"), String::toString);
		List<String> languages = split(delim, data.get("language"), String::toString);

		List<Person> actors = new ArrayList<Person>();
		actors.addAll(split(delim, data.get("actors"), (s) -> new Person(s, null, null)));
		actors.addAll(split(delim, data.get("director"), (s) -> new Person(s, null, "Director")));
		actors.addAll(split(delim, data.get("writer"), (s) -> new Person(s, null, "Writer")));

		return new MovieInfo(fields, emptyList(), genres, emptyMap(), languages, emptyList(), emptyList(), actors, emptyList());
	}

	private SimpleDate parsePartialDate(String value, String format) {
		if (value != null && value.length() > 0) {
			try {
				TemporalAccessor f = DateTimeFormatter.ofPattern(format, Locale.ENGLISH).parse(value);
				if (f.isSupported(ChronoField.YEAR)) {
					if (f.isSupported(ChronoField.MONTH_OF_YEAR) && f.isSupported(ChronoField.DAY_OF_MONTH)) {
						return new SimpleDate(f.get(ChronoField.YEAR), f.get(ChronoField.MONTH_OF_YEAR), f.get(ChronoField.DAY_OF_MONTH));
					} else {
						return new SimpleDate(f.get(ChronoField.YEAR), 1, 1);
					}
				}
			} catch (DateTimeParseException e) {
				debug.warning(format("Bad date: %s =~ %s => %s", value, format, e));
			}
		}
		return null;
	}

	private <T> List<T> split(Pattern regex, String value, Function<String, T> toObject) {
		if (value == null || value.isEmpty())
			return emptyList();

		return regex.splitAsStream(value).map(String::trim).filter(s -> !s.equals("N/A")).map(toObject).collect(toList());
	}

}
