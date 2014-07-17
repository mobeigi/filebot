package net.filebot.media;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class XattrMetaInfoProvider {

	public String getName() {
		return "xattr";
	}

	public Map<File, Object> getMetaData(Iterable<File> files) {
		Map<File, Object> result = new LinkedHashMap<File, Object>();

		for (File f : files) {
			Object metaObject = MediaDetection.readMetaInfo(f);
			if (metaObject != null) {
				result.put(f, metaObject);
			}
		}

		return result;
	}

}
