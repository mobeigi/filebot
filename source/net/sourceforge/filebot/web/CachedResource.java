
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
	
	private Cache cache;
	private String resource;
	private Class<T> type;
	private long expirationTime;
	
	
	public CachedResource(String resource, Class<T> type, long expirationTime) {
		this.cache = CacheManager.getInstance().getCache("web-persistent-datasource");
		this.resource = resource;
		this.type = type;
		this.expirationTime = expirationTime;
	}
	
	
	/**
	 * Convert resource data into usable data
	 */
	public abstract T process(ByteBuffer data) throws Exception;
	
	
	public synchronized T get() throws IOException {
		String cacheKey = type.getName() + ":" + resource.toString();
		
		Element element = cache.get(cacheKey);
		long lastUpdateTime = (element != null) ? element.getLatestOfCreationAndUpdateTime() : 0;
		
		// fetch from cache
		if (element != null && System.currentTimeMillis() - lastUpdateTime < expirationTime) {
			try {
				return type.cast(element.getValue());
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		// fetch and process resource
		ByteBuffer data = fetchIfModified(new URL(resource), element != null ? lastUpdateTime : 0);
		
		if (data != null) {
			try {
				element = new Element(cacheKey, process(data));
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
		
		// update cached data and last-updated time
		cache.put(element);
		return type.cast(element.getValue());
	}
	
}
