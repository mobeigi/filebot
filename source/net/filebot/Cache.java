package net.filebot;

import static net.filebot.Logging.*;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import net.sf.ehcache.Element;

public class Cache {

	public static Cache getCache(String name, CacheType type) {
		return CacheManager.getInstance().getCache(name.toLowerCase(), type);
	}

	private final net.sf.ehcache.Cache cache;

	public Cache(net.sf.ehcache.Cache cache) {
		this.cache = cache;
	}

	public Object get(Object key) {
		try {
			return cache.get(key).getObjectValue();
		} catch (Exception e) {
			debug.warning(format("Bad cache state: %s => %s", key, e));
		}
		return null;
	}

	public Object computeIf(Object key, Predicate<Element> condition, Callable<?> callable) throws Exception {
		// get if present
		try {
			Element element = cache.get(key);
			if (element != null && condition.test(element)) {
				return element.getObjectValue();
			}
		} catch (Exception e) {
			debug.warning(format("Bad cache state: %s => %s", key, e));
		}

		// compute if absent
		Object value = callable.call();
		try {
			cache.put(new Element(key, value));
		} catch (Exception e) {
			debug.warning(format("Bad cache state: %s => %s", key, e));
		}
		return value;
	}

	public Object computeIfAbsent(Object key, Callable<?> callable) throws Exception {
		return computeIf(key, Element::isExpired, callable);
	}

	public Object computeIfStale(Object key, Duration expirationTime, Callable<?> callable) throws Exception {
		return computeIf(key, isStale(expirationTime), callable);
	}

	private Predicate<Element> isStale(Duration expirationTime) {
		return (element) -> element.isExpired() || System.currentTimeMillis() - element.getLatestOfCreationAndUpdateTime() < expirationTime.toMillis();
	}

	public void put(Object key, Object value) {
		try {
			cache.put(new Element(key, value));
		} catch (Exception e) {
			debug.warning(format("Bad cache state: %s => %s", key, e));
		}
	}

	public void remove(Object key) {
		try {
			cache.remove(key);
		} catch (Exception e) {
			debug.warning(format("Bad cache state: %s => %s", key, e));
		}
	}

	@Deprecated
	public <T> T get(Object key, Class<T> type) {
		return type.cast(get(key));
	}

	@Deprecated
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
