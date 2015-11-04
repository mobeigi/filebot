package net.filebot.web;

import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.filebot.web.FanartTVClient.FanartDescriptor.FanartProperty;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

public class FanartTVClient {

	private String apikey;

	public FanartTVClient(String apikey) {
		this.apikey = apikey;
	}

	public List<FanartDescriptor> getSeriesArtwork(int tvdbid) throws Exception {
		return getArtwork("tv", String.valueOf(tvdbid));
	}

	public List<FanartDescriptor> getMovieArtwork(int tmdbid) throws Exception {
		return getArtwork("movies", String.valueOf(tmdbid));
	}

	public List<FanartDescriptor> getArtwork(String category, String id) throws Exception {
		String resource = getResource(category, id);

		// cache results
		CachedResource<FanartDescriptor[]> data = new CachedResource<FanartDescriptor[]>(resource, FanartDescriptor[].class, CachedResource.ONE_WEEK) {

			@Override
			public FanartDescriptor[] process(ByteBuffer data) throws Exception {
				String json = Charset.forName("UTF-8").decode(data).toString();
				Map<?, ?> maps = JsonReader.jsonToMaps(json);

				List<FanartDescriptor> fanart = new ArrayList<FanartDescriptor>();
				maps.forEach((k, v) -> {
					if (v instanceof JsonObject) {
						JsonObject<?, ?> mapping = (JsonObject<?, ?>) v;
						if (mapping.isArray()) {
							for (Object i : mapping.getArray()) {
								if (i instanceof Map) {
									Map<?, ?> item = (Map<?, ?>) i;
									Map<FanartProperty, String> fields = new EnumMap<FanartProperty, String>(FanartProperty.class);
									fields.put(FanartProperty.type, k.toString());
									for (FanartProperty prop : FanartProperty.values()) {
										Object value = item.get(prop.name());
										if (value != null) {
											fields.put(prop, value.toString());
										}
									}
									if (fields.size() > 1) {
										fanart.add(new FanartDescriptor(fields));
									}
								}
							}
						}
					}
				});

				return fanart.toArray(new FanartDescriptor[0]);
			}

			@Override
			protected Cache getCache() {
				return CacheManager.getInstance().getCache("web-datasource-lv2");
			}
		};

		return Arrays.asList(data.get());
	}

	public String getResource(String category, String id) {
		// e.g. http://webservice.fanart.tv/v3/movies/17645?api_key=6fa42b0ef3b5f3aab6a7edaa78675ac2
		return "http://webservice.fanart.tv/v3/" + category + "/" + id + "?api_key=" + apikey;
	}

	public static class FanartDescriptor implements Serializable {

		public static enum FanartProperty {
			type, id, url, lang, likes, season, disc, disc_type
		}

		protected Map<FanartProperty, String> fields;

		protected FanartDescriptor() {
			// used by serializer
		}

		protected FanartDescriptor(Map<FanartProperty, String> fields) {
			this.fields = new EnumMap<FanartProperty, String>(fields);
		}

		public String get(Object key) {
			return fields.get(FanartProperty.valueOf(key.toString()));
		}

		public String get(FanartProperty key) {
			return fields.get(key);
		}

		public String getType() {
			return fields.get(FanartProperty.type);
		}

		public Integer getId() {
			try {
				return new Integer(fields.get(FanartProperty.id));
			} catch (Exception e) {
				return null;
			}
		}

		public URL getUrl() {
			try {
				return new URL(fields.get(FanartProperty.url));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getLikes() {
			try {
				return new Integer(fields.get(FanartProperty.likes));
			} catch (Exception e) {
				return null;
			}
		}

		public Locale getLanguage() {
			try {
				return new Locale(fields.get(FanartProperty.lang));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getSeason() {
			try {
				return new Integer(fields.get(FanartProperty.season));
			} catch (Exception e) {
				return null;
			}
		}

		public Integer getDiskNumber() {
			try {
				return new Integer(fields.get(FanartProperty.disc));
			} catch (Exception e) {
				return null;
			}
		}

		public String getDiskType() {
			return fields.get(FanartProperty.disc_type);
		}

		@Override
		public String toString() {
			return fields.toString();
		}
	}

}
