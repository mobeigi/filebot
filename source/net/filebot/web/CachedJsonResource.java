package net.filebot.web;

import static net.filebot.util.JsonUtilities.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class CachedJsonResource extends AbstractCachedResource<String, String> {

	public CachedJsonResource(String resource) {
		super(resource, String.class, ONE_DAY, 2, 1000);
	}

	@Override
	protected Cache getCache() {
		return CacheManager.getInstance().getCache("web-datasource-lv3");
	}

	public Object getJsonObject() throws IOException {
		try {
			return readJson(get());
		} catch (Exception e) {
			throw new IOException(String.format("Error while loading JSON resource: %s (%s)", getResourceLocation(resource), e.getMessage()));
		}
	}

	@Override
	public String process(String data) throws IOException {
		try {
			readJson(get()); // make sure JSON is valid
			return data;
		} catch (Exception e) {
			throw new IOException(String.format("Malformed JSON: %s (%s)", getResourceLocation(resource), e.getMessage()));
		}
	}

	@Override
	protected String fetchData(URL url, long lastModified) throws IOException {
		ByteBuffer data = WebRequest.fetchIfModified(url, lastModified);

		if (data == null)
			return null; // not modified

		return StandardCharsets.UTF_8.decode(data).toString();
	}

}
