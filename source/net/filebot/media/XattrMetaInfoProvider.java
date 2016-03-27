package net.filebot.media;

import static net.filebot.media.XattrMetaInfo.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Icon;

import net.filebot.web.Datasource;

public class XattrMetaInfoProvider implements Datasource {

	@Override
	public String getName() {
		return "xattr";
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	public Map<File, Object> getMetaData(Iterable<File> files) {
		Map<File, Object> result = new LinkedHashMap<File, Object>();

		for (File f : files) {
			Object metaObject = xattr.getMetaInfo(f);
			if (metaObject != null) {
				result.put(f, metaObject);
			}
		}

		return result;
	}

}
