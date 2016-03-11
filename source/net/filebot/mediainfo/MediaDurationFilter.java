package net.filebot.mediainfo;

import static net.filebot.Logging.*;

import java.io.File;
import java.io.FileFilter;

import net.filebot.mediainfo.MediaInfo.StreamKind;

public class MediaDurationFilter implements FileFilter {

	private final MediaInfo mediaInfo = new MediaInfo();

	private final long min;
	private final long max;
	private final boolean acceptByDefault;

	public MediaDurationFilter(long min) {
		this(min, Long.MAX_VALUE, false);
	}

	public MediaDurationFilter(long min, long max, boolean acceptByDefault) {
		this.min = min;
		this.max = max;
		this.acceptByDefault = acceptByDefault;
	}

	public long getDuration(File file) {
		synchronized (mediaInfo) {
			try {
				String duration = mediaInfo.open(file).get(StreamKind.General, 0, "Duration");
				return Long.parseLong(duration);
			} catch (Exception e) {
				debug.warning("Failed to read video duration: " + e.getMessage());
			}
		}
		return -1;
	}

	@Override
	public boolean accept(File file) {
		long d = getDuration(file);
		if (d >= 0) {
			return d >= min && d <= max;
		}
		return acceptByDefault;
	}
}
