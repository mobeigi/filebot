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

public class CachedResource2<K, R> implements Resource<R> {

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

	@Override
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
	public interface Transform<T, R> {
		R transform(T object) throws Exception;
	}

	public static Transform<ByteBuffer, byte[]> getBytes() {
		return (data) -> {
			byte[] bytes = new byte[data.remaining()];
			data.get(bytes, 0, bytes.length);
			return bytes;
		};
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

	@FunctionalInterface
	public interface Fetch {
		ByteBuffer fetch(URL url, long lastModified) throws Exception;
	}

	public static Fetch fetchIfModified() {
		return (url, lastModified) -> {
			try {
				debug.fine(WebRequest.log(url, lastModified, null));
				return WebRequest.fetchIfModified(url, lastModified);
			} catch (FileNotFoundException e) {
				return fileNotFound(url, e);
			}
		};
	}

	public static Fetch fetchIfNoneMatch(Cache etagStorage) {
		return (url, lastModified) -> {
			// record ETag response header
			Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();

			String etagKey = url.toString();
			Object etagValue = etagStorage.get(etagKey);

			try {
				debug.fine(WebRequest.log(url, lastModified, etagValue));
				return WebRequest.fetch(url, lastModified, etagValue, null, responseHeaders);
			} catch (FileNotFoundException e) {
				return fileNotFound(url, e);
			} finally {
				List<String> value = responseHeaders.get("ETag");
				if (value != null && value.size() > 0 && !value.contains(etagValue)) {
					etagStorage.put(etagKey, value.get(0));
				}
			}
		};
	}

	private static ByteBuffer fileNotFound(URL url, FileNotFoundException e) {
		debug.warning(format("Resource not found: %s => %s", url, e.getMessage()));
		return ByteBuffer.allocate(0);
	}

	@FunctionalInterface
	public interface Permit {
		void acquire(URL resource) throws Exception;
	}

	public static Fetch withPermit(Fetch fetch, Permit permit) {
		return (url, lastModified) -> {
			permit.acquire(url);
			return fetch.fetch(url, lastModified);
		};
	}

}
