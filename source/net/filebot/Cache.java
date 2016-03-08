package net.filebot;

import static java.nio.charset.StandardCharsets.*;
import static net.filebot.CachedResource2.*;
import static net.filebot.Logging.*;

import java.io.Serializable;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Predicate;

import net.filebot.CachedResource2.Transform;
import net.sf.ehcache.Element;

import org.w3c.dom.Document;

public class Cache {

	public static final Duration ONE_DAY = Duration.ofDays(1);
	public static final Duration ONE_WEEK = Duration.ofDays(7);

	public static Cache getCache(String name, CacheType type) {
		return CacheManager.getInstance().getCache(name, type);
	}

	public <T> CachedResource2<T, String> text(T key, Transform<T, URL> resource) {
		return new CachedResource2<T, String>(key, resource, fetchIfModified(), getText(UTF_8), String.class::cast, ONE_DAY, this);
	}

	public <T> CachedResource2<T, Document> xml(T key, Transform<T, URL> resource) {
		return new CachedResource2<T, Document>(key, resource, fetchIfModified(), validateXml(getText(UTF_8)), getXml(String.class::cast), ONE_DAY, this);
	}

	public <T> CachedResource2<T, Object> json(T key, Transform<T, URL> resource) {
		return new CachedResource2<T, Object>(key, resource, fetchIfModified(), validateJson(getText(UTF_8)), getJson(String.class::cast), ONE_DAY, this);
	}

	private final net.sf.ehcache.Cache cache;

	public Cache(net.sf.ehcache.Cache cache) {
		this.cache = cache;
	}

	public Object get(Object key) {
		try {
			Element element = cache.get(key);
			if (element != null) {
				return element.getObjectValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
			debug.warning(format("Cache get: %s => %s", key, e));
		}
		return null;
	}

	public Object computeIf(Object key, Predicate<Element> condition, Compute<?> compute) throws Exception {
		// get if present
		Element element = null;
		try {
			element = cache.get(key);
			if (element != null && condition.test(element)) {
				return element.getObjectValue();
			}
		} catch (Exception e) {
			debug.warning(format("Cache get: %s => %s", key, e));
		}

		// compute if absent
		Object value = compute.apply(element);
		try {
			cache.put(new Element(key, value));
		} catch (Exception e) {
			debug.warning(format("Cache put: %s => %s", key, e));
		}
		return value;
	}

	public Object computeIfAbsent(Object key, Compute<?> compute) throws Exception {
		return computeIf(key, isAbsent(), compute);
	}

	public Object computeIfStale(Object key, Duration expirationTime, Compute<?> compute) throws Exception {
		return computeIf(key, isStale(expirationTime), compute);
	}

	public Predicate<Element> isAbsent() {
		return (element) -> element.getObjectValue() == null;
	}

	public Predicate<Element> isStale(Duration expirationTime) {
		return (element) -> System.currentTimeMillis() - element.getLatestOfCreationAndUpdateTime() < expirationTime.toMillis();
	}

	public void put(Object key, Object value) {
		try {
			cache.put(new Element(key, value));
		} catch (Exception e) {
			debug.warning(format("Cache put: %s => %s", key, e));
		}
	}

	public void remove(Object key) {
		try {
			cache.remove(key);
		} catch (Exception e) {
			debug.warning(format("Cache remove: %s => %s", key, e));
		}
	}

	@FunctionalInterface
	public interface Compute<R> {
		R apply(Element element) throws Exception;
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
