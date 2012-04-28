
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
	
	
	public CachedResource(String resource, Class<T> type, long expirationTime) {
		this.resource = resource;
		this.type = type;
		this.expirationTime = expirationTime;
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
			if (element != null) {
				lastUpdateTime = element.getLatestOfCreationAndUpdateTime();
			}
			
			// fetch from cache
			if (element != null && System.currentTimeMillis() - lastUpdateTime < expirationTime) {
				return type.cast(element.getValue());
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
		}
		
		// fetch and process resource
		ByteBuffer data = fetchData(new URL(resource), element != null ? lastUpdateTime : 0);
		T product = null;
		
		if (data != null) {
			try {
				product = process(data);
				element = new Element(cacheKey, product);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
		
		try {
			getCache().put(element);
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
		}
		
		return product;
	}
	
}
