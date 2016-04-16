package net.filebot.media;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.ExceptionUtilities.*;

import java.io.File;
import java.util.Locale;

import net.filebot.Cache;
import net.filebot.Cache.Compute;
import net.filebot.CacheType;
import net.filebot.Resource;
import net.filebot.WebServices;
import net.filebot.web.Episode;
import net.filebot.web.Movie;
import net.filebot.web.SimpleDate;

public class XattrMetaInfo {

	public static final XattrMetaInfo xattr = new XattrMetaInfo(useExtendedFileAttributes(), useCreationDate());

	private final boolean useExtendedFileAttributes;
	private final boolean useCreationDate;

	private final Cache xattrMetaInfoCache = Cache.getCache(MetaAttributes.METADATA_KEY, CacheType.Ephemeral);
	private final Cache xattrOriginalNameCache = Cache.getCache(MetaAttributes.FILENAME_KEY, CacheType.Ephemeral);

	public XattrMetaInfo(boolean useExtendedFileAttributes, boolean useCreationDate) {
		this.useExtendedFileAttributes = useExtendedFileAttributes;
		this.useCreationDate = useCreationDate;
	}

	public boolean isMetaInfo(Object object) {
		return object instanceof Episode || object instanceof Movie;
	}

	public long getTimeStamp(Object object) throws Exception {
		if (object instanceof Episode) {
			Episode episode = (Episode) object;
			if (episode.getAirdate() != null) {
				return episode.getAirdate().getTimeStamp();
			}
		} else if (object instanceof Movie) {
			Movie movie = (Movie) object;
			if (movie.getYear() > 0 && movie.getTmdbId() > 0) {
				SimpleDate releaseDate = WebServices.TheMovieDB.getMovieInfo(movie, Locale.ENGLISH, false).getReleased();
				if (releaseDate != null) {
					return releaseDate.getTimeStamp();
				}
			}
		}
		return -1;
	}

	public synchronized Object getMetaInfo(File file) {
		return getCachedValue(xattrMetaInfoCache, file, key -> {
			return xattr(file).getObject();
		});
	}

	public synchronized String getOriginalName(File file) {
		return (String) getCachedValue(xattrOriginalNameCache, file, key -> {
			return xattr(file).getOriginalName();
		});
	}

	private MetaAttributes xattr(File file) throws Exception {
		return new MetaAttributes(file);
	}

	private Object getCachedValue(Cache cache, File file, Compute<?> compute) {
		// try in-memory cache of previously stored xattr metadata
		try {
			return cache.computeIfAbsent(file, key -> {
				if (useExtendedFileAttributes) {
					return compute.apply(key);
				}
				return null;
			});
		} catch (Throwable e) {
			debug.warning("Failed to read xattr: " + getRootCauseMessage(e));
		}
		return null;
	}

	public synchronized void setMetaInfo(File file, Object model, String original) {
		// only for Episode / Movie objects
		if (!isMetaInfo(model) || !file.isFile()) {
			return;
		}

		// set creation date to episode / movie release date
		Resource<MetaAttributes> xattr = Resource.lazy(() -> xattr(file));

		if (useCreationDate) {
			try {
				long t = getTimeStamp(model);
				if (t > 0) {
					xattr.get().setCreationDate(t);
				}
			} catch (Throwable e) {
				debug.warning("Failed to set creation date: " + getRootCauseMessage(e));
			}
		}

		// store metadata object and original name as xattr
		try {
			if (isMetaInfo(model)) {
				xattrMetaInfoCache.put(file, model);

				if (useExtendedFileAttributes) {
					xattr.get().setObject(model);
				}
			}

			if (original != null && original.length() > 0 && getOriginalName(file) == null) {
				xattrOriginalNameCache.put(file, original);

				if (useExtendedFileAttributes) {
					xattr.get().setOriginalName(original);
				}
			}
		} catch (Throwable e) {
			debug.warning("Failed to set xattr: " + getRootCauseMessage(e));
		}
	}

	public synchronized void clear(File file) {
		// clear in-memory cache
		xattrMetaInfoCache.remove(file);
		xattrOriginalNameCache.remove(file);

		if (useExtendedFileAttributes) {
			try {
				xattr(file).clear();
			} catch (Throwable e) {
				debug.warning("Failed to clear xattr: " + getRootCauseMessage(e));
			}
		}
	}

}
