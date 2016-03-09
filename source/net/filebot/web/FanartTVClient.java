package net.filebot.web;

import static java.util.stream.Collectors.*;
import static net.filebot.util.JsonUtilities.*;

import java.io.Serializable;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.web.FanartTVClient.FanartDescriptor.FanartProperty;

public class FanartTVClient implements Datasource {

	private String apikey;

	public FanartTVClient(String apikey) {
		this.apikey = apikey;
	}

	@Override
	public String getName() {
		return "FanartTV";
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	public List<FanartDescriptor> getSeriesArtwork(int tvdbid) throws Exception {
		return getArtwork("tv", String.valueOf(tvdbid));
	}

	public List<FanartDescriptor> getMovieArtwork(int tmdbid) throws Exception {
		return getArtwork("movies", String.valueOf(tmdbid));
	}

	public List<FanartDescriptor> getArtwork(String category, String id) throws Exception {
		String path = category + '/' + id;

		Cache cache = Cache.getCache(getName(), CacheType.Weekly);
		Object json = cache.json(path, s -> getResource(s)).expire(Cache.ONE_WEEK);

		return asMap(json).entrySet().stream().flatMap(type -> {
			return streamJsonObjects(type.getValue()).map(item -> {
				Map<FanartProperty, String> map = getEnumMap(item, FanartProperty.class);
				map.put(FanartProperty.type, type.getKey().toString());

				return new FanartDescriptor(map);
			}).filter(art -> art.getUrl() != null);
		}).collect(toList());
	}

	public URL getResource(String path) throws Exception {
		// e.g. http://webservice.fanart.tv/v3/movies/17645?api_key=6fa42b0ef3b5f3aab6a7edaa78675ac2
		return new URL("http://webservice.fanart.tv/v3/" + path + "?api_key=" + apikey);
	}

	public static class FanartDescriptor implements Serializable {

		public static enum FanartProperty {
			type, id, url, lang, likes, season, disc, disc_type
		}

		protected Map<FanartProperty, String> properties;

		protected FanartDescriptor() {
			// used by serializer
		}

		protected FanartDescriptor(Map<FanartProperty, String> fields) {
			this.properties = new EnumMap<FanartProperty, String>(fields);
		}

		public String get(Object key) {
			return properties.get(FanartProperty.valueOf(key.toString()));
		}

		public String get(FanartProperty key) {
			return properties.get(key);
		}

		public String getType() {
			return properties.get(FanartProperty.type);
		}

		public Integer getId() {
			try {
				return new Integer(properties.get(FanartProperty.id));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getUrl() {
			try {
				return new URL(properties.get(FanartProperty.url));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getLikes() {
			try {
				return new Integer(properties.get(FanartProperty.likes));
			} catch (Exception e) {
				return null;
			}
		}

		public Locale getLanguage() {
			try {
				return new Locale(properties.get(FanartProperty.lang));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getSeason() {
			try {
				return new Integer(properties.get(FanartProperty.season));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getDiskNumber() {
			try {
				return new Integer(properties.get(FanartProperty.disc));
			} catch (Exception e) {
				return null;
			}
		}

		public String getDiskType() {
			return properties.get(FanartProperty.disc_type);
		}

		@Override
		public String toString() {
			return properties.toString();
		}
	}

}
