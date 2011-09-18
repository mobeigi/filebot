
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;


public abstract class CachedResource<T extends Serializable> {
	
	private Cache cache;
	private String resource;
	private long expirationTime;
	

	public CachedResource(String resource, long expirationTime) {
		this.cache = CacheManager.getInstance().getCache("web-persistent-datasource");
		this.resource = resource;
		this.expirationTime = expirationTime;
	}
	

	/**
	 * Convert resource data into usable data
	 */
	public abstract T process(ByteBuffer data);
	

	@SuppressWarnings("unchecked")
	public synchronized T get() throws IOException {
		Element element = cache.get(resource);
		long lastUpdateTime = (element != null) ? element.getLatestOfCreationAndUpdateTime() : 0;
		
		if (element == null || System.currentTimeMillis() - lastUpdateTime > expirationTime) {
			ByteBuffer data = fetchIfModified(new URL(resource), element != null ? lastUpdateTime : 0);
			
			if (data != null) {
				element = new Element(resource, process(data));
			}
			
			// update cached data and last-updated time
			cache.put(element);
		}
		
		return (T) element.getValue();
	}
	
}
