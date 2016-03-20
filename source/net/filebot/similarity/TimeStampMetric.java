
package net.filebot.similarity;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

public class TimeStampMetric implements SimilarityMetric {

	@Override
	public float getSimilarity(Object o1, Object o2) {
		long t1 = getTimeStamp(o1);
		long t2 = getTimeStamp(o2);

		if (t1 <= 0 || t2 <= 0)
			return -1;

		float min = Math.min(t1, t2);
		float max = Math.max(t1, t2);

		return min / max;
	}

	public long getTimeStamp(Object obj) {
		if (obj instanceof File) {
			try {
				BasicFileAttributes attr = Files.readAttributes(((File) obj).toPath(), BasicFileAttributes.class);
				long creationTime = attr.creationTime().toMillis();
				if (creationTime > 0) {
					return creationTime;
				} else {
					return attr.lastModifiedTime().toMillis();
				}
			} catch (Throwable e) {
				// ignore Java 6 issues
				return ((File) obj).lastModified();
			}
		} else if (obj instanceof Number) {
			return ((Number) obj).longValue();
		}

		return -1;
	}
}
