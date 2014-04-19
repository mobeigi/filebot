package net.sourceforge.filebot.mediainfo;

import java.io.File;
import java.io.FileFilter;

import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;

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
			if (mediaInfo.open(file)) {
				try {
					return Long.parseLong(mediaInfo.get(StreamKind.General, 0, "Duration"));
				} catch (NumberFormatException e) {
					// ignore, assume duration couldn't be read
				}
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
