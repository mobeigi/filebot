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

public class CachedResource2<K, R> {

	public static final int DEFAULT_RETRY_LIMIT = 2;
	public static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(2);

	protected final K key;

	protected final Source<K> source;
	protected final Fetch fetch;
	protected final Parse<R> parse;

	protected final Duration expirationTime;

	protected final int retryCountLimit;
	protected final long retryWaitTime;

	protected final Cache cache;

	public CachedResource2(K key, Source<K> source, Fetch fetch, Parse<R> parse, Duration expirationTime, Cache cache) {
		this(key, source, fetch, parse, DEFAULT_RETRY_LIMIT, DEFAULT_RETRY_DELAY, expirationTime, cache);
	}

	public CachedResource2(K key, Source<K> source, Fetch fetch, Parse<R> parse, int retryCountLimit, Duration retryWaitTime, Duration expirationTime, Cache cache) {
		this.key = key;
		this.source = source;
		this.fetch = fetch;
		this.parse = parse;
		this.expirationTime = expirationTime;
		this.retryCountLimit = retryCountLimit;
		this.retryWaitTime = retryWaitTime.toMillis();
		this.cache = cache;
	}

	@SuppressWarnings("unchecked")
	public synchronized R get() throws Exception {
		return (R) cache.computeIfStale(key, expirationTime, element -> {
			URL resource = source.source(key);
			long lastModified = element == null ? 0 : element.getLatestOfCreationAndUpdateTime();

			debug.fine(format("Fetch %s (If-Modified-Since: %tc)", resource, lastModified));

			try {
				ByteBuffer data = retry(() -> fetch.fetch(resource, lastModified), retryCountLimit, lastModified);

				// 304 Not Modified
				if (data == null && element != null && element.getObjectValue() != null) {
					return element.getObjectValue();
				}

				return parse.parse(data);
			} catch (IOException e) {
				debug.fine(format("Fetch failed => %s", e));

				// use previously cached data if possible
				if (element == null || element.getObjectValue() == null) {
					throw e;
				}
				return element.getObjectKey();
			}
		});
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
	public interface Parse<R> {
		R parse(ByteBuffer bytes) throws Exception;
	}

	public static Parse<String> decode(Charset charset) {
		return (bb) -> charset.decode(bb).toString();
	}

	public static Fetch fetchIfModified(FloodLimit limit) {
		return (url, lastModified) -> {
			try {
				limit.acquirePermit();
				return WebRequest.fetchIfModified(url, lastModified);
			} catch (FileNotFoundException e) {
				return ByteBuffer.allocate(0);
			}
		};
	}

}
