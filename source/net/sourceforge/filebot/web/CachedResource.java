
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;


public abstract class CachedResource<T extends Serializable> {
	
	private String resource;
	private Class<T> type;
	private long expirationTime;
	
	private int retryCountLimit;
	private long retryWaitTime;
	
	
	public CachedResource(String resource, Class<T> type) {
		this(resource, type, Long.MAX_VALUE);
	}
	
	
	public CachedResource(String resource, Class<T> type, long expirationTime) {
		this(resource, type, expirationTime, 2, 1000);
	}
	
	
	public CachedResource(String resource, Class<T> type, long expirationTime, int retryCountLimit, long retryWaitTime) {
		this.resource = resource;
		this.type = type;
		this.expirationTime = expirationTime;
		this.retryCountLimit = retryCountLimit;
		this.retryWaitTime = retryWaitTime;
	}
	
	
	protected Cache getCache() {
		return CacheManager.getInstance().getCache("web-persistent-datasource");
	}
	
	
	protected ByteBuffer fetchData(URL url, long lastModified) throws IOException {
		return fetchIfModified(url, lastModified);
	}
	
	
	/**
	 * Convert resource data into usable data
	 */
	public abstract T process(ByteBuffer data) throws Exception;
	
	
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
		ByteBuffer data = null;
		T product = null;
		IOException networkException = null;
		
		try {
			long lastModified = element != null ? lastUpdateTime : 0;
			URL url = new URL(resource);
			data = fetch(url, lastModified);
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
	
	
	protected ByteBuffer fetch(URL url, long lastModified) throws IOException, InterruptedException {
		for (int i = 0; retryCountLimit < 0 || i <= retryCountLimit; i++) {
			try {
				return fetchData(url, lastModified);
			} catch (IOException e) {
				if (i >= 0 && i >= retryCountLimit) {
					throw e;
				}
				Thread.sleep(retryWaitTime);
			}
		}
		return null; // can't happen
	}
}
