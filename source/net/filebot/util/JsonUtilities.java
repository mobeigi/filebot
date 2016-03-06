package net.filebot.util;

import java.util.Map;

import com.cedarsoftware.util.io.JsonReader;

public class JsonUtilities {

	public static Map<?, ?> readJson(String json) {
		return JsonReader.jsonToMaps(json);
	}

	public static Object[] getArray(Object node, String key) {
		return (Object[]) ((Map<?, ?>) node).get(key);
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
