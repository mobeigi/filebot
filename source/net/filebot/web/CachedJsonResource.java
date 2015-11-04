package net.filebot.web;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

public class CachedJsonResource extends AbstractCachedResource<String, String> {

	public CachedJsonResource(String resource) {
		super(resource, String.class, ONE_DAY, 2, 1000);
	}

	@Override
	protected Cache getCache() {
		return CacheManager.getInstance().getCache("web-datasource-lv3");
	}

	public JsonObject<?, ?> getJSON() throws IOException {
		try {
			return (JsonObject<?, ?>) JsonReader.jsonToMaps(get());
		} catch (Exception e) {
			throw new IOException(String.format("Error while loading JSON resource: %s (%s)", getResourceLocation(resource), e.getMessage()));
		}
	}

	@Override
	public String process(String data) throws IOException {
		try {
			JsonReader.jsonToMaps(data); // make sure JSON is valid
		} catch (Exception e) {
			throw new IOException(String.format("Malformed JSON: %s (%s)", getResourceLocation(resource), e.getMessage()));
		}
		return data;
	}

	@Override
	protected String fetchData(URL url, long lastModified) throws IOException {
		ByteBuffer data = WebRequest.fetchIfModified(url, lastModified);

		if (data == null)
			return null; // not modified

		return StandardCharsets.UTF_8.decode(data).toString();
	}

}
