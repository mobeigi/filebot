package net.filebot;

import static net.filebot.Logging.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import net.filebot.util.JsonUtilities;
import net.filebot.web.WebRequest;

import org.w3c.dom.Document;

public class CachedResource2<K, R> {

	public static final int DEFAULT_RETRY_LIMIT = 2;
	public static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(2);

	private K key;

	private Transform<K, URL> resource;
	private Fetch fetch;
	private Transform<ByteBuffer, ? extends Object> parse;
	private Transform<? super Object, R> cast;

	private Duration expirationTime;

	private int retryLimit;
	private Duration retryWait;

	private final Cache cache;

	public CachedResource2(K key, Transform<K, URL> resource, Fetch fetch, Transform<ByteBuffer, ? extends Object> parse, Transform<? super Object, R> cast, Duration expirationTime, Cache cache) {
		this(key, resource, fetch, parse, cast, DEFAULT_RETRY_LIMIT, DEFAULT_RETRY_DELAY, expirationTime, cache);
	}

	public CachedResource2(K key, Transform<K, URL> resource, Fetch fetch, Transform<ByteBuffer, ? extends Object> parse, Transform<? super Object, R> cast, int retryLimit, Duration retryWait, Duration expirationTime, Cache cache) {
		this.key = key;
		this.resource = resource;
		this.fetch = fetch;
		this.parse = parse;
		this.cast = cast;
		this.expirationTime = expirationTime;
		this.retryLimit = retryLimit;
		this.retryWait = retryWait;
		this.cache = cache;
	}

	public synchronized CachedResource2<K, R> fetch(Fetch fetch) {
		this.fetch = fetch;
		return this;
	}

	public synchronized CachedResource2<K, R> expire(Duration expirationTime) {
		this.expirationTime = expirationTime;
		return this;
	}

	public synchronized CachedResource2<K, R> retry(int retryLimit) {
		this.retryLimit = retryLimit;
		return this;
	}

	public synchronized R get() throws Exception {
		Object value = cache.computeIfStale(key, expirationTime, element -> {
			URL url = resource.transform(key);
			long lastModified = element == null ? 0 : element.getLatestOfCreationAndUpdateTime();

			try {
				ByteBuffer data = retry(() -> fetch.fetch(url, lastModified), retryLimit, retryWait);

				// 304 Not Modified
				if (data == null && element != null && element.getObjectValue() != null) {
					return element.getObjectValue();
				}

				return parse.transform(data);
			} catch (IOException e) {
				debug.fine(format("Fetch failed => %s", e));

				// use previously cached data if possible
				if (element == null || element.getObjectValue() == null) {
					throw e;
				}
				return element.getObjectKey();
			}
		});

		return cast.transform(value);
	}

	protected <T> T retry(Callable<T> callable, int retryCount, Duration retryWaitTime) throws Exception {
		try {
			return callable.call();
		} catch (FileNotFoundException e) {
			// resource does not exist, do not retry
			throw e;
		} catch (IOException e) {
			// retry or rethrow exception
			if (retryCount > 0) {
				throw e;
			}
			Thread.sleep(retryWaitTime.toMillis());
			return retry(callable, retryCount - 1, retryWaitTime.multipliedBy(2));
		}
	}

	@FunctionalInterface
	public interface Fetch {
		ByteBuffer fetch(URL url, long lastModified) throws Exception;
	}

	@FunctionalInterface
	public interface Transform<T, R> {
		R transform(T object) throws Exception;
	}

	@FunctionalInterface
	public interface Permit<P> {
		boolean acquirePermit(URL resource) throws Exception;
	}

	public static Transform<ByteBuffer, String> getText(Charset charset) {
		return (data) -> charset.decode(data).toString();
	}

	public static <T> Transform<T, String> validateXml(Transform<T, String> parse) {
		return (object) -> {
			String xml = parse.transform(object);
			WebRequest.validateXml(xml);
			return xml;
		};
	}

	public static <T> Transform<T, String> validateJson(Transform<T, String> parse) {
		return (object) -> {
			String json = parse.transform(object);
			JsonUtilities.readJson(json);
			return json;
		};
	}

	public static <T> Transform<T, Document> getXml(Transform<T, String> parse) {
		return (object) -> {
			return WebRequest.getDocument(parse.transform(object));
		};
	}

	public static <T> Transform<T, Object> getJson(Transform<T, String> parse) {
		return (object) -> {
			return JsonUtilities.readJson(parse.transform(object));
		};
	}

	public static Fetch fetchIfModified() {
		return (url, lastModified) -> {
			try {
				debug.fine(format("Fetch %s (If-Modified-Since: %tc)", url, lastModified));
				return WebRequest.fetchIfModified(url, lastModified);
			} catch (FileNotFoundException e) {
				debug.warning(format("Resource not found: %s => %s", url, e));
				return ByteBuffer.allocate(0);
			}
		};
	}

	public static Fetch fetchIfNoneMatch(Cache etagStorage) {
		return (url, lastModified) -> {
			Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();

			String etagKey = url.toString();
			Object etagValue = etagStorage.get(etagKey);

			try {
				debug.fine(format("Fetch %s (If-None-Match: %s, If-Modified-Since: %tc)", url, etagValue, lastModified));
				return WebRequest.fetch(url, lastModified, etagValue, null, responseHeaders);
			} catch (FileNotFoundException e) {
				debug.warning(format("Resource not found: %s => %s", url, e));
				return ByteBuffer.allocate(0);
			} finally {
				List<String> value = responseHeaders.get("ETag");
				if (value != null && value.size() > 0 && !value.contains(etagValue)) {
					debug.finest(format("ETag %s", value));
					etagStorage.put(etagKey, value.get(0));
				}
			}
		};
	}

	public static Fetch withPermit(Fetch fetch, Permit<?> permit) {
		return (url, lastModified) -> {
			if (permit.acquirePermit(url)) {
				return fetch.fetch(url, lastModified);
			}
			return null;
		};
	}

}
