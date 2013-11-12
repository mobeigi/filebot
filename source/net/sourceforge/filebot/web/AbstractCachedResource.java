package net.sourceforge.filebot.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public abstract class AbstractCachedResource<R, T extends Serializable> {

	public static final long ONE_MINUTE = 60 * 1000;
	public static final long ONE_HOUR = 60 * ONE_MINUTE;
	public static final long ONE_DAY = 24 * ONE_HOUR;
	public static final long ONE_WEEK = 7 * ONE_DAY;
	public static final long ONE_MONTH = 30 * ONE_DAY;

	private String resource;
	private Class<T> type;
	private long expirationTime;

	private int retryCountLimit;
	private long retryWaitTime;

	public AbstractCachedResource(String resource, Class<T> type, long expirationTime, int retryCountLimit, long retryWaitTime) {
		this.resource = resource;
		this.type = type;
		this.expirationTime = expirationTime;
		this.retryCountLimit = retryCountLimit;
		this.retryWaitTime = retryWaitTime;
	}

	/**
	 * Convert resource data into usable data
	 */
	protected abstract R fetchData(URL url, long lastModified) throws IOException;

	protected abstract T process(R data) throws Exception;

	protected abstract Cache getCache();

	public synchronized T get() throws IOException {
		String cacheKey = type.getName() + ":" + resource.toString();
		Element element = null;
		long lastUpdateTime = 0;

		try {
			element = getCache().get(cacheKey);

			// sanity check ehcache diskcache problems
			if (element != null && !cacheKey.equals(element.getKey().toString())) {
				element = null;
			}

			if (element != null) {
				lastUpdateTime = element.getLatestOfCreationAndUpdateTime();
			}

			// fetch from cache
			if (element != null && System.currentTimeMillis() - lastUpdateTime < expirationTime) {
				return type.cast(element.getValue());
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.FINEST, e.getMessage());
		}

		// fetch and process resource
		R data = null;
		T product = null;
		IOException networkException = null;

		try {
			long lastModified = element != null ? lastUpdateTime : 0;
			URL url = getResourceLocation(resource);
			data = fetch(url, lastModified, element != null ? 0 : retryCountLimit);
		} catch (IOException e) {
			networkException = e;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		if (data != null) {
			try {
				product = process(data);
				element = new Element(cacheKey, product);
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else {
			try {
				if (element != null) {
					product = type.cast(element.getValue());
				}
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.FINEST, e.getMessage());
			}
		}

		try {
			if (element != null) {
				getCache().put(element);
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.FINEST, e.getMessage());
		}

		// throw network error only if we can't use previously cached data
		if (networkException != null) {
			if (product == null) {
				throw networkException;
			}

			// just log error and continue with cached data
			Logger.getLogger(getClass().getName()).log(Level.WARNING, networkException.toString());
		}

		return product;
	}

	protected URL getResourceLocation(String resource) throws IOException {
		return new URL(resource);
	}

	protected R fetch(URL url, long lastModified, int retries) throws IOException, InterruptedException {
		for (int i = 0; retries < 0 || i <= retries; i++) {
			try {
				if (i > 0) {
					Thread.sleep(retryWaitTime);
				}
				return fetchData(url, lastModified);
			} catch (FileNotFoundException e) {
				// if the resource doesn't exist no need for retries
				throw e;
			} catch (IOException e) {
				if (i >= 0 && i >= retries) {
					throw e;
				}
			}
		}
		return null; // can't happen
	}
}
