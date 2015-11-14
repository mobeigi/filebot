package net.filebot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class Cache {

	public static final String EPHEMERAL = "ephemeral";

	public static Cache getCache(String name) {
		return new Cache(CacheManager.getInstance().getCache(name));
	}

	private final net.sf.ehcache.Cache cache;

	protected Cache(net.sf.ehcache.Cache cache) {
		this.cache = cache;
	}

	public void put(Object key, Object value) {
		try {
			cache.put(new Element(key, value));
		} catch (Throwable e) {
			Logger.getLogger(Cache.class.getName()).log(Level.WARNING, e.getMessage());
			remove(key); // fail-safe
		}
	}

	public Object get(Object key) {
		return get(key, Object.class);
	}

	public <T> T get(Object key, Class<T> type) {
		try {
			Element element = cache.get(key);
			if (element != null && key.equals(element.getKey())) {
				return type.cast(element.getValue());
			}
		} catch (Exception e) {
			Logger.getLogger(Cache.class.getName()).log(Level.WARNING, e.getMessage());
			remove(key); // fail-safe
		}

		return null;
	}

	public void remove(Object key) {
		try {
			cache.remove(key);
		} catch (Exception e1) {
			Logger.getLogger(Cache.class.getName()).log(Level.SEVERE, e1.getMessage());
			try {
				Logger.getLogger(Cache.class.getName()).log(Level.INFO, "Cached data has become invalid: Clearing cache now");
				cache.removeAll();
			} catch (Exception e2) {
				Logger.getLogger(Cache.class.getName()).log(Level.SEVERE, e2.getMessage());
				try {
					Logger.getLogger(Cache.class.getName()).log(Level.INFO, "Cache has become invalid: Reset all caches");
					cache.getCacheManager().clearAll();
				} catch (Exception e3) {
					Logger.getLogger(Cache.class.getName()).log(Level.SEVERE, e3.getMessage());
				}
			}
		}
	}

	public static class Key implements Serializable {

		protected Object[] fields;

		public Key(Object... fields) {
			this.fields = fields;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(fields);
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Key) {
				return Arrays.equals(this.fields, ((Key) other).fields);
			}

			return false;
		}

		@Override
		public String toString() {
			return Arrays.toString(fields);
		}
	}

}
