package net.filebot.mediainfo;

import static net.filebot.Logging.*;

import java.io.File;
import java.io.FileFilter;

import net.filebot.mediainfo.MediaInfo.StreamKind;

public class MediaDurationFilter implements FileFilter {

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

	public long getDuration(File f) {
		try (MediaInfo mi = new MediaInfo().open(f)) {
			String duration = mi.get(StreamKind.General, 0, "Duration");
			if (duration.length() > 0) {
				return Long.parseLong(duration);
			}
		} catch (Exception e) {
			debug.warning(format("Failed to read video duration: %s", e.getMessage()));
		}
		return -1;
	}

	@Override
	public boolean accept(File f) {
		long d = getDuration(f);
		if (d >= 0) {
			return d >= min && d <= max;
		}
		return acceptByDefault;
	}
}
