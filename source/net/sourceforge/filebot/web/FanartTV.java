package net.sourceforge.filebot.web;

import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.web.FanartTV.FanartDescriptor.FanartProperty;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class FanartTV {

	private String apikey;

	public FanartTV(String apikey) {
		this.apikey = apikey;
	}

	public List<FanartDescriptor> getSeriesArtwork(int tvdbid) throws Exception {
		return getSeriesArtwork(String.valueOf(tvdbid), "all", 1, 2);
	}

	public List<FanartDescriptor> getSeriesArtwork(String id, String type, int sort, int limit) throws Exception {
		return getArtwork("series", id, type, sort, limit);
	}

	public List<FanartDescriptor> getMovieArtwork(int tmdbid) throws Exception {
		return getMovieArtwork(String.valueOf(tmdbid), "all", 1, 2);
	}

	public List<FanartDescriptor> getMovieArtwork(String id, String type, int sort, int limit) throws Exception {
		return getArtwork("movie", id, type, sort, limit);
	}

	public List<FanartDescriptor> getArtwork(String category, String id, String type, int sort, int limit) throws Exception {
		String resource = getResource(category, id, "xml", type, sort, limit);

		// cache results
		CachedResource<FanartDescriptor[]> data = new CachedResource<FanartDescriptor[]>(resource, FanartDescriptor[].class) {

			@Override
			public FanartDescriptor[] process(ByteBuffer data) throws Exception {
				Document dom = getDocument(Charset.forName("UTF-8").decode(data).toString());

				List<FanartDescriptor> fanart = new ArrayList<FanartDescriptor>();
				for (Node node : selectNodes("//*[@url]", dom)) {
					// e.g. <seasonthumb id="3481" url="http://fanart.tv/fanart/tv/70327/seasonthumb/3481/Buffy (6).jpg" lang="en" likes="0" season="6"/>
					Map<FanartProperty, String> fields = new EnumMap<FanartProperty, String>(FanartProperty.class);
					fields.put(FanartProperty.type, node.getNodeName());
					for (FanartProperty prop : FanartProperty.values()) {
						String value = getAttribute(prop.name(), node);
						if (value != null) {
							fields.put(prop, value);
						}
					}
					fanart.add(new FanartDescriptor(fields));
				}

				return fanart.toArray(new FanartDescriptor[0]);
			}

			@Override
			protected Cache getCache() {
				return CacheManager.getInstance().getCache("web-datasource-lv2");
			}
		};

		return Arrays.asList(data.get());
	}

	public String getResource(String category, String id, String format, String type, int sort, int limit) throws MalformedURLException {
		// e.g. http://fanart.tv/webservice/series/780b986b22c35e6f7a134a2f392c2deb/70327/xml/all/1/2
		return String.format("http://api.fanart.tv/webservice/%s/%s/%s/%s/%s/%s/%s", category, apikey, id, format, type, sort, limit);
	}

	public static class FanartDescriptor implements Serializable {

		public static enum FanartProperty {
			type, id, url, lang, likes, season, disc_type
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

		public String getName() {
			return new File(getUrl().getFile()).getName();
		}

		public URL getUrl() {
			try {
				return new URL(fields.get(FanartProperty.url).replaceAll(" ", "%20")); // work around server-side url encoding issues
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

		public String getDiskType() {
			return fields.get(FanartProperty.disc_type);
		}

		@Override
		public String toString() {
			return fields.toString();
		}
	}

}
