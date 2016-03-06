package net.filebot.util;

import static java.util.Collections.*;

import java.util.Map;

import com.cedarsoftware.util.io.JsonReader;

public class JsonUtilities {

	public static Map<?, ?> readJson(CharSequence json) {
		return (Map<?, ?>) JsonReader.jsonToJava(json.toString(), singletonMap(JsonReader.USE_MAPS, true));
	}

	public static Map<?, ?> asMap(Object node) {
		if (node instanceof Map) {
			return (Map<?, ?>) node;
		}
		return null;
	}

	public static Object[] asArray(Object node) {
		if (node instanceof Object[]) {
			return (Object[]) node;
		}
		return null;
	}

	public static Object[] getArray(Object node, String key) {
		return asArray(((Map<?, ?>) node).get(key));
	}

	public static Map<?, ?> getFirstMap(Object node, String key) {
		Object[] values = getArray(node, key);
		if (values != null && values.length > 0) {
			return (Map<?, ?>) values[0];
		}
		return null;
	}

	public static String getString(Object node, String key) {
		Object value = ((Map<?, ?>) node).get(key);
		if (value != null) {
			return value.toString();
		}
		return null;
	}

	public static Integer getInteger(Object node, String key) {
		String value = getString(node, key);
		if (value != null && value.length() > 0) {
			return new Integer(value.toString());
		}
		return null;
	}

}
