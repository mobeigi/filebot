
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sourceforge.filebot.web.FanartTV.FanartDescriptor.FanartProperty;


public class FanartTV {
	
	private String apikey;
	
	
	public FanartTV(String apikey) {
		this.apikey = apikey;
	}
	
	
	public List<FanartDescriptor> getArtwork(int tvdbid) throws Exception {
		return getArtwork(tvdbid, "all", 1, 2);
	}
	
	
	public List<FanartDescriptor> getArtwork(int tvdbid, String type, int sort, int limit) throws Exception {
		String xml = new CachedPage(getResource(tvdbid, "xml", type, sort, limit)).get();
		Document dom = getDocument(xml);
		
		List<FanartDescriptor> fanart = new ArrayList<FanartDescriptor>();
		
		for (Node node : selectNodes("//*[@url]", dom)) {
			// e.g. <seasonthumb id="3481" url="http://fanart.tv/fanart/tv/70327/seasonthumb/3481/Buffy (6).jpg" lang="en" likes="0" season="6"/>
			Map<FanartProperty, String> data = new EnumMap<FanartProperty, String>(FanartProperty.class);
			data.put(FanartProperty.type, node.getNodeName());
			for (FanartProperty prop : FanartProperty.values()) {
				String value = getAttribute(prop.name(), node);
				if (value != null) {
					data.put(prop, value);
				}
			}
			fanart.add(new FanartDescriptor(data));
		}
		
		return fanart;
	}
	
	
	public URL getResource(int tvdbid, String format, String type, int sort, int limit) throws MalformedURLException {
		// e.g. http://fanart.tv/webservice/series/780b986b22c35e6f7a134a2f392c2deb/70327/xml/all/1/2
		return new URL(String.format("http://fanart.tv/webservice/series/%s/%s/%s/%s/%s/%s", apikey, tvdbid, format, type, sort, limit));
	}
	
	
	public static class FanartDescriptor implements Serializable {
		
		public static enum FanartProperty {
			type,
			id,
			url,
			lang,
			likes,
			season
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
		
		
		public Integer getSeason() {
			try {
				return new Integer(fields.get(FanartProperty.season));
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
