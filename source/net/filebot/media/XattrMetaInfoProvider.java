package net.filebot.media;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.web.Datasource;

public class XattrMetaInfoProvider implements Datasource {

	@Override
	public String getName() {
		return "xattr";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.xattr");
	}

	public Map<File, Object> match(Collection<File> files, boolean strict) {
		// enable xattr regardless of -DuseExtendedFileAttributes system properties
		XattrMetaInfo xattr = new XattrMetaInfo(true, false);

		Map<File, Object> result = new LinkedHashMap<File, Object>();

		for (File f : files) {
			Object object = xattr.getMetaInfo(f);

			if (object != null) {
				result.put(f, object);
			} else if (!strict) {
				result.put(f, f);
			}
		}

		return result;
	}

}
