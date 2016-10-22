package net.filebot.util.prefs;

import static java.nio.charset.StandardCharsets.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyFileBackingStore {

	private Path store;
	private int modCount = 0;

	private Map<String, Map<String, String>> nodes = new HashMap<String, Map<String, String>>();

	public PropertyFileBackingStore(Path store) {
		this.store = store;
	}

	private Map<String, String> newKeyValueMap(String node) {
		return new HashMap<String, String>();
	}

	public synchronized String setValue(String node, String key, String value) {
		modCount++;
		return nodes.computeIfAbsent(node, this::newKeyValueMap).put(key, value);
	}

	public synchronized String getValue(String node, String key) {
		Map<String, String> values = nodes.get(node);
		if (values != null) {
			return values.get(key);
		}
		return null;
	}

	public synchronized void removeValue(String node, String key) {
		Map<String, String> values = nodes.get(node);
		if (values != null) {
			modCount++;
			values.remove(key);
		}
	}

	public synchronized void removeNode(String node) {
		modCount++;
		nodes.remove(node);
	}

	public synchronized String[] getKeys(String node) {
		Map<String, String> values = nodes.get(node);
		if (values != null) {
			return values.keySet().toArray(new String[0]);
		}
		return new String[0];
	}

	public synchronized String[] getChildren(String node) {
		return nodes.keySet().stream().filter(k -> k.length() > node.length() && k.indexOf('/', node.length()) < 0 && k.startsWith(node)).map(k -> k.substring(node.length())).toArray(String[]::new);
	}

	public synchronized Properties toProperties() {
		Properties props = new Properties();

		nodes.forEach((node, values) -> {
			values.forEach((key, value) -> {
				props.put(node + '/' + key, value);
			});
		});

		return props;
	}

	public synchronized void mergeNodes(Map<String, Map<String, String>> n) {
		n.forEach((node, values) -> {
			nodes.merge(node, values, (m1, m2) -> {
				Map<String, String> m = newKeyValueMap(node);
				m.putAll(m1);
				m.putAll(m2);
				return m;
			});
		});
	}

	public void sync() throws IOException {
		if (!Files.exists(store)) {
			return;
		}

		byte[] bytes = Files.readAllBytes(store);
		StringReader buffer = new StringReader(new String(bytes, UTF_8));

		Properties props = new Properties();
		props.load(buffer);

		Map<String, Map<String, String>> n = new HashMap<String, Map<String, String>>();

		props.forEach((k, v) -> {
			String propertyKey = k.toString();
			int s = propertyKey.lastIndexOf('/');

			String node = propertyKey.substring(0, s);
			String key = propertyKey.substring(s + 1);

			n.computeIfAbsent(node, this::newKeyValueMap).put(key, v.toString());
		});

		mergeNodes(n);
	}

	public void flush() throws IOException {
		if (modCount == 0) {
			return;
		}

		StringWriter buffer = new StringWriter();
		toProperties().store(buffer, null);

		ByteBuffer data = UTF_8.encode(CharBuffer.wrap(buffer.getBuffer()));

		try (FileChannel out = FileChannel.open(store, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
			try (FileLock lock = out.lock()) {
				out.write(data);
				out.truncate(out.position());
			}
		}

		modCount = 0; // reset
	}

	@Override
	public synchronized String toString() {
		return nodes.toString();
	}

}
