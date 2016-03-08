package net.filebot;

import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.Files.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.filebot.mac.xattr.XAttrUtil;

import com.sun.jna.Platform;

public class MetaAttributeView extends AbstractMap<String, String> {

	// UserDefinedFileAttributeView (for Windows and Linux) OR our own xattr.h JNA wrapper via MacXattrView (for Mac) because UserDefinedFileAttributeView is not supported (Oracle Java 7/8)
	private Object xattr;
	private Charset encoding;

	public MetaAttributeView(File file) throws IOException {
		Path path = file.getCanonicalFile().toPath();
		while (isSymbolicLink(path)) {
			Path link = readSymbolicLink(path);
			if (!link.isAbsolute()) {
				link = path.getParent().resolve(link);
			}
			path = link;
		}

		xattr = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);

		if (xattr == null) {
			if (Platform.isMac()) {
				xattr = new MacXattrView(path);
			} else {
				throw new IOException("UserDefinedFileAttributeView is not supported");
			}
		} else {
			// UserDefinedFileAttributeView
			this.encoding = UTF_8;
		}
	}

	@Override
	public String get(Object key) {
		try {
			if (xattr instanceof UserDefinedFileAttributeView) {
				UserDefinedFileAttributeView attributeView = (UserDefinedFileAttributeView) xattr;
				ByteBuffer buffer = ByteBuffer.allocate(attributeView.size(key.toString()));
				attributeView.read(key.toString(), buffer);
				buffer.flip();

				return encoding.decode(buffer).toString();
			}

			if (xattr instanceof MacXattrView) {
				MacXattrView macXattr = (MacXattrView) xattr;
				return macXattr.read(key.toString());
			}
		} catch (Exception e) {
			// ignore
		}

		return null;
	}

	@Override
	public String put(String key, String value) {
		try {
			if (xattr instanceof UserDefinedFileAttributeView) {
				UserDefinedFileAttributeView attributeView = (UserDefinedFileAttributeView) xattr;
				if (value == null || value.isEmpty()) {
					attributeView.delete(key);
				} else {
					attributeView.write(key, encoding.encode(value));
				}
			}

			if (xattr instanceof MacXattrView) {
				MacXattrView macXattr = (MacXattrView) xattr;
				if (value == null || value.isEmpty()) {
					macXattr.delete(key);
				} else {
					macXattr.write(key, value);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return null; // since we don't know the old value
	}

	public List<String> list() throws IOException {
		if (xattr instanceof UserDefinedFileAttributeView) {
			UserDefinedFileAttributeView attributeView = (UserDefinedFileAttributeView) xattr;
			return attributeView.list();
		}

		if (xattr instanceof MacXattrView) {
			MacXattrView macXattr = (MacXattrView) xattr;
			return macXattr.list();
		}

		return null;
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		try {
			Set<Entry<String, String>> entries = new LinkedHashSet<Entry<String, String>>();
			for (String name : this.list()) {
				entries.add(new AttributeEntry(name));
			}
			return entries;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clear() {
		try {
			for (String key : this.list()) {
				this.put(key, null);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private class AttributeEntry implements Entry<String, String> {

		private final String name;

		public AttributeEntry(String name) {
			this.name = name;
		}

		@Override
		public String getKey() {
			return name;
		}

		@Override
		public String getValue() {
			return get(name);
		}

		@Override
		public String setValue(String value) {
			return put(name, value);
		}

		@Override
		public String toString() {
			return getKey() + "=" + getValue();
		}
	}

	private static class MacXattrView {

		private final String path;

		public MacXattrView(Path path) {
			// MacOS filesystem may require NFD unicode decomposition
			this.path = Normalizer.normalize(path.toFile().getAbsolutePath(), Form.NFD);
		}

		public List<String> list() {
			return XAttrUtil.listXAttr(path);
		}

		public String read(String key) {
			return XAttrUtil.getXAttr(path, key);
		}

		public void write(String key, String value) {
			XAttrUtil.setXAttr(path, key, value);
		}

		public void delete(String key) {
			XAttrUtil.removeXAttr(path, key);
		}
	}

}
