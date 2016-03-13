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
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.w3c.dom.Document;

import net.filebot.util.JsonUtilities;
import net.filebot.web.WebRequest;

public class CachedResource<K, R> implements Resource<R> {

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

	public CachedResource(K key, Transform<K, URL> resource, Fetch fetch, Transform<ByteBuffer, ? extends Object> parse, Transform<? super Object, R> cast, Duration expirationTime, Cache cache) {
		this(key, resource, fetch, parse, cast, DEFAULT_RETRY_LIMIT, DEFAULT_RETRY_DELAY, expirationTime, cache);
	}

	public CachedResource(K key, Transform<K, URL> resource, Fetch fetch, Transform<ByteBuffer, ? extends Object> parse, Transform<? super Object, R> cast, int retryLimit, Duration retryWait, Duration expirationTime, Cache cache) {
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

	public synchronized CachedResource<K, R> fetch(Fetch fetch) {
		this.fetch = fetch;
		return this;
	}

	public synchronized CachedResource<K, R> expire(Duration expirationTime) {
		this.expirationTime = expirationTime;
		return this;
	}

	public synchronized CachedResource<K, R> retry(int retryLimit) {
		this.retryLimit = retryLimit;
		return this;
	}

	@Override
	public synchronized R get() throws Exception {
		Object value = cache.computeIf(key, Cache.isStale(expirationTime), element -> {
			URL url = resource.transform(key);
			long lastModified = element == null ? 0 : element.getLatestOfCreationAndUpdateTime();

			try {
				ByteBuffer data = retry(() -> fetch.fetch(url, lastModified), retryLimit, retryWait);
				debug.finest(format("Received %,d bytes", data == null ? 0 : data.remaining()));

				// 304 Not Modified
				if (data == null && element != null && element.getObjectValue() != null) {
					return element.getObjectValue();
				}

				return parse.transform(data);
			} catch (IOException e) {
				debug.warning(format("Fetch failed: %s", e.getMessage()));

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

			debug.fine(format("Fetch failed: Retry %d => %s", retryCount, e.getMessage()));
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

	public static Fetch fetchIfNoneMatch(Function<URL, Object> etagRetrieve, BiConsumer<URL, String> etagStore) {
		return (url, lastModified) -> {
			// record ETag response header
			Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
			Object etagValue = etagRetrieve.apply(url);

			try {
				debug.fine(WebRequest.log(url, lastModified, etagValue));
				if (etagValue != null) {
					return WebRequest.fetch(url, 0, etagValue, null, responseHeaders);
				} else {
					return WebRequest.fetch(url, lastModified, null, null, responseHeaders);
				}
			} catch (FileNotFoundException e) {
				return fileNotFound(url, e);
			} finally {
				WebRequest.getETag(responseHeaders).ifPresent(etag -> {
					debug.finest(format("Store ETag: %s", etag));
					etagStore.accept(url, etag);
				});
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
