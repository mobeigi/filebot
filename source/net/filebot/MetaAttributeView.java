package net.filebot;

import static java.nio.file.Files.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.securityvision.xattrj.Xattrj;

import com.sun.jna.Platform;

public class MetaAttributeView extends AbstractMap<String, String> {

	// UserDefinedFileAttributeView (for Windows and Linux) OR 3rd party Xattrj (for Mac) because UserDefinedFileAttributeView is not supported (Oracle Java 7/8)
	private Object xattr;
	private File file;
	private Charset encoding;

	private static Xattrj MacXattrSupport;

	private static synchronized Xattrj getMacXattrSupport() throws Throwable {
		if (MacXattrSupport == null) {
			MacXattrSupport = new Xattrj();
		}
		return MacXattrSupport;
	}

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

		// try 3rd party Xattrj library
		if (xattr == null) {
			if (Platform.isMac()) {
				try {
					xattr = getMacXattrSupport();
				} catch (Throwable e) {
					throw new IOException("Unable to load library: xattrj", e);
				}
			} else {
				throw new IOException("UserDefinedFileAttributeView is not supported");
			}
		}

		this.file = path.toFile();
		this.encoding = Charset.forName("UTF-8");
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

			if (xattr instanceof Xattrj) {
				Xattrj macXattr = (Xattrj) xattr;
				return macXattr.readAttribute(file, key.toString());
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

			if (xattr instanceof Xattrj) {
				Xattrj macXattr = (Xattrj) xattr;
				if (value == null || value.isEmpty()) {
					macXattr.removeAttribute(file, key);
				} else {
					macXattr.writeAttribute(file, key, value);
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		return null; // since we don't know the old value
	}

	public List<String> list() throws IOException {
		if (xattr instanceof UserDefinedFileAttributeView) {
			UserDefinedFileAttributeView attributeView = (UserDefinedFileAttributeView) xattr;
			return attributeView.list();
		}

		if (xattr instanceof Xattrj) {
			Xattrj macXattr = (Xattrj) xattr;
			return Arrays.asList(macXattr.listAttributes(file));
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
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void clear() {
		try {
			for (String key : this.list()) {
				this.put(key, null);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
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

}
