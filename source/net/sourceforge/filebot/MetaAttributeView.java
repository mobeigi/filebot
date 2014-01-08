package net.sourceforge.filebot;

import static java.nio.file.Files.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class MetaAttributeView extends AbstractMap<String, String> {

	private final UserDefinedFileAttributeView attributeView;
	private final Charset encoding;

	public MetaAttributeView(File file) throws IOException {
		Path path = file.getCanonicalFile().toPath();
		while (isSymbolicLink(path)) {
			Path link = readSymbolicLink(path);
			if (!link.isAbsolute()) {
				link = path.getParent().resolve(link);
			}
			path = link;
		}

		attributeView = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		encoding = Charset.forName("UTF-8");
	}

	@Override
	public String get(Object key) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(attributeView.size(key.toString()));
			attributeView.read(key.toString(), buffer);
			buffer.flip();

			return encoding.decode(buffer).toString();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String put(String key, String value) {
		try {
			if (value == null || value.isEmpty()) {
				attributeView.delete(key);
			} else {
				attributeView.write(key, encoding.encode(value));
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return null; // since we don't know the old value
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		try {
			Set<Entry<String, String>> entries = new LinkedHashSet<Entry<String, String>>();
			for (String name : attributeView.list()) {
				entries.add(new AttributeEntry(name));
			}
			return entries;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void clear() {
		try {
			for (String it : attributeView.list()) {
				attributeView.delete(it);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public int size() {
		try {
			return attributeView.list().size();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean isEmpty() {
		try {
			return attributeView.list().isEmpty();
		} catch (IOException e) {
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
