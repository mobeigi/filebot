package net.filebot.media;

import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;

import java.io.File;
import java.util.Comparator;

import net.filebot.format.MediaBindingBean;

public class MediaSize implements Comparator<File> {

	public static final Comparator<File> VIDEO_SIZE_ORDER = new MediaSize();

	@Override
	public int compare(File f1, File f2) {
		long[] v1 = getSizeValues(f1);
		long[] v2 = getSizeValues(f2);

		// best to worst
		for (int i = 0; i < v1.length; i++) {
			int d = Long.compare(v1[i], v2[i]);
			if (d != 0) {
				return d;
			}
		}

		return 0;
	}

	public long[] getSizeValues(File f) {
		long[] v = { -1, -1 };

		if (VIDEO_FILES.accept(f) || SUBTITLE_FILES.accept(f)) {
			try {
				MediaBindingBean media = new MediaBindingBean(null, f, null);
				v[1] = media.getInferredMediaFile().length(); // File Size
				v[0] = media.getDimension().stream().mapToLong(Number::longValue).reduce((a, b) -> a * b).orElse(-1); // Video Resolution
			} catch (Throwable e) {
				debug.warning(format("Failed to read media info: %s [%s]", e.getMessage(), f.getName()));
			}
		} else {
			v[1] = f.length(); // File Size
		}

		return v;
	}

}
