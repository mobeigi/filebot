package net.filebot.media;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import net.filebot.WebServices;
import net.filebot.web.Episode;
import net.filebot.web.Movie;
import net.filebot.web.SimpleDate;

public class XattrMetaInfo {

	public static final XattrMetaInfo xattr = new XattrMetaInfo(useExtendedFileAttributes(), useCreationDate());

	private final boolean useExtendedFileAttributes;
	private final boolean useCreationDate;

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

	public Object readMetaInfo(File file) {
		if (useExtendedFileAttributes) {
			try {
				MetaAttributes attr = new MetaAttributes(file);
				Object metadata = attr.getObject();
				if (isMetaInfo(metadata)) {
					return metadata;
				}
			} catch (Throwable e) {
				debug.warning("Failed to read xattr: " + e.getMessage());
			}
		}
		return null;
	}

	public void storeMetaInfo(File file, Object model, String original) {
		// only for Episode / Movie objects
		if ((useExtendedFileAttributes || useCreationDate) && isMetaInfo(model) && file.isFile()) {
			try {
				MetaAttributes attr = new MetaAttributes(file);

				// set creation date to episode / movie release date
				if (useCreationDate) {
					try {
						long t = getTimeStamp(model);
						if (t > 0) {
							attr.setCreationDate(t);
						}
					} catch (Throwable e) {
						if (e.getCause() instanceof IOException) {
							e = e.getCause();
						}
						debug.warning("Failed to set creation date: " + e.getMessage());
					}
				}

				// store original name and model as xattr
				if (useExtendedFileAttributes) {
					try {
						if (isMetaInfo(model)) {
							attr.setObject(model);
						}
						if (attr.getOriginalName() == null && original != null && original.length() > 0) {
							attr.setOriginalName(original);
						}
					} catch (Throwable e) {
						if (e.getCause() instanceof IOException) {
							e = e.getCause();
						}
						debug.warning("Failed to set xattr: " + e.getMessage());
					}
				}
			} catch (Throwable t) {
				debug.warning("Failed to store xattr: " + t.getMessage());
			}
		}
	}

}
