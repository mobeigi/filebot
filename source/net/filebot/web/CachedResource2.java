package net.filebot.web;

import static net.filebot.Logging.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.Callable;

import net.filebot.Cache;

import org.w3c.dom.Document;

public class CachedResource2<K, R> implements Resource<R> {

	public static final int DEFAULT_RETRY_LIMIT = 2;
	public static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(2);

	protected final K key;

	protected final Source<K> source;
	protected final Fetch fetch;
	protected final Transform<ByteBuffer, ? extends Object> parse;
	protected final Transform<? super Object, R> cast;

	protected final Duration expirationTime;

	protected final int retryCountLimit;
	protected final long retryWaitTime;

	protected final Cache cache;

	public CachedResource2(K key, Source<K> source, Fetch fetch, Transform<ByteBuffer, ? extends Object> parse, Transform<? super Object, R> cast, int retryCountLimit, Duration retryWaitTime, Duration expirationTime, Cache cache) {
		this.key = key;
		this.source = source;
		this.fetch = fetch;
		this.parse = parse;
		this.cast = cast;
		this.expirationTime = expirationTime;
		this.retryCountLimit = retryCountLimit;
		this.retryWaitTime = retryWaitTime.toMillis();
		this.cache = cache;
	}

	@Override
	public synchronized R get() throws Exception {
		Object value = cache.computeIfStale(key, expirationTime, element -> {
			URL resource = source.source(key);
			long lastModified = element == null ? 0 : element.getLatestOfCreationAndUpdateTime();

			debug.fine(format("Fetch %s (If-Modified-Since: %tc)", resource, lastModified));

			try {
				ByteBuffer data = retry(() -> fetch.fetch(resource, lastModified), retryCountLimit, lastModified);

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

	protected <T> T retry(Callable<T> callable, int retryCount, long retryWaitTime) throws Exception {
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
			Thread.sleep(retryWaitTime);
			return retry(callable, retryCount - 1, retryWaitTime * 2);
		}
	}

	@FunctionalInterface
	public interface Source<K> {
		URL source(K key) throws Exception;
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

	public static <T> Transform<T, Document> getXml(Transform<T, String> parse) {
		return (object) -> {
			return WebRequest.getDocument(parse.transform(object));
		};
	}

	public static Fetch fetchIfModified() {
		return (url, lastModified) -> {
			try {
				return WebRequest.fetchIfModified(url, lastModified);
			} catch (FileNotFoundException e) {
				debug.warning(format("Resource not found: %s => %s", url, e));
				return ByteBuffer.allocate(0);
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
