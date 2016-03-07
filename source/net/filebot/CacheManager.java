package net.filebot;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

public class CacheManager {

	private static final CacheManager instance = new CacheManager();

	public static CacheManager getInstance() {
		return instance;
	}

	private final net.sf.ehcache.CacheManager manager;

	public CacheManager() {
		try {
			this.manager = net.sf.ehcache.CacheManager.create(getConfiguration());
		} catch (IOException e) {
			throw new CacheException(e);
		}
	}

	public Cache getCache(String name, CacheType type) {
		String cacheName = name.toLowerCase() + "_" + type.ordinal();
		if (!manager.cacheExists(cacheName)) {
			debug.config("Create cache: " + cacheName);
			manager.addCache(new net.sf.ehcache.Cache(type.getConfiguration(cacheName)));
		}
		return new Cache(manager.getCache(cacheName));
	}

	public void clearAll() {
		manager.clearAll();
	}

	private Configuration getConfiguration() throws IOException {
		Configuration config = new Configuration();
		config.addDiskStore(getDiskStoreConfiguration());
		return config;
	}

	private DiskStoreConfiguration getDiskStoreConfiguration() throws IOException {
		// prepare cache folder for this application instance
		File cacheRoot = getApplicationCache().getCanonicalFile();

		for (int i = 0; true; i++) {
			File cache = new File(cacheRoot, Integer.toString(i));

			// make sure cache is readable and writable
			createFolders(cache);

			final File lockFile = new File(cache, ".lock");
			boolean isNewCache = !lockFile.exists();

			final FileChannel channel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
			final FileLock lock = channel.tryLock();

			if (lock != null) {
				debug.config(format("Using persistent disk cache %s", cache));

				int applicationRevision = getApplicationRevisionNumber();
				int cacheRevision = 0;
				try {
					cacheRevision = new Scanner(channel, "UTF-8").nextInt();
				} catch (Exception e) {
					// ignore
				}

				if (cacheRevision != applicationRevision && applicationRevision > 0 && !isNewCache) {
					debug.config(format("App version (r%d) does not match cache version (r%d): reset cache", applicationRevision, cacheRevision));

					// tag cache with new revision number
					isNewCache = true;

					// delete all files related to previous cache instances
					for (File it : getChildren(cache)) {
						if (!it.equals(lockFile)) {
							delete(cache);
						}
					}
				}

				if (isNewCache) {
					// set new cache revision
					channel.position(0);
					channel.write(Charset.forName("UTF-8").encode(String.valueOf(applicationRevision)));
					channel.truncate(channel.position());
				}

				// make sure to orderly shutdown cache
				Runtime.getRuntime().addShutdownHook(new Thread() {

					@Override
					public void run() {
						try {
							manager.shutdown();
						} catch (Exception e) {
							// ignore, shutting down anyway
						}
						try {
							lock.release();
						} catch (Exception e) {
							// ignore, shutting down anyway
						}
						try {
							channel.close();
						} catch (Exception e) {
							// ignore, shutting down anyway
						}
					}
				});

				// cache for this application instance is successfully set up and locked
				return new DiskStoreConfiguration().path(cache.getPath());
			}

			// try next lock file
			channel.close();
		}
	}

}
