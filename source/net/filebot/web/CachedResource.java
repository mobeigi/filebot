package net.filebot.web;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public abstract class CachedResource<T extends Serializable> extends AbstractCachedResource<ByteBuffer, T> {

	public CachedResource(String resource, Class<T> type, long expirationTime) {
		this(resource, type, expirationTime, 2, 1000); // 3 retries in 1s intervals by default
	}

	public CachedResource(String resource, Class<T> type, long expirationTime, int retryCountLimit, long retryWaitTime) {
		super(resource, type, expirationTime, retryCountLimit, retryWaitTime);
	}

	@Override
	protected Cache getCache() {
		return CacheManager.getInstance().getCache("web-persistent-datasource");
	}

	@Override
	protected ByteBuffer fetchData(URL url, long lastModified) throws IOException {
		return WebRequest.fetchIfModified(url, lastModified);
	}

}
