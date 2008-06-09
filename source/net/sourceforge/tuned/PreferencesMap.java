
package net.sourceforge.tuned;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class PreferencesMap<T> implements Map<String, T> {
	
	private final Preferences prefs;
	private final Adapter<T> adapter;
	
	
	public PreferencesMap(Preferences prefs, Adapter<T> adapter) {
		this.prefs = prefs;
		this.adapter = adapter;
	}
	

	@Override
	public T get(Object key) {
		if (key instanceof String) {
			return adapter.get(prefs, (String) key);
		}
		
		throw new IllegalArgumentException("Key must be a String");
	}
	

	@Override
	public T put(String key, T value) {
		adapter.put(prefs, key, value);
		
		return value;
	}
	

	/**
	 * @return always null
	 */
	@Override
	public T remove(Object key) {
		if (key instanceof String) {
			prefs.remove((String) key);
		}
		
		return null;
	}
	

	public String[] keys() {
		try {
			return prefs.keys();
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public void clear() {
		try {
			prefs.clear();
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
	}
	

	public void set(Map<String, T> data) {
		clear();
		putAll(data);
	}
	

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String) {
			return Arrays.asList(keys()).contains(key);
		}
		
		return false;
	}
	

	@Override
	public boolean containsValue(Object value) {
		for (String key : keys()) {
			if (value.equals(get(key)))
				return true;
		}
		
		return false;
	}
	

	@Override
	public Set<Map.Entry<String, T>> entrySet() {
		Set<Map.Entry<String, T>> entries = new LinkedHashSet<Map.Entry<String, T>>();
		
		for (String key : keys()) {
			entries.add(new Entry(key));
		}
		
		return entries;
	}
	

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	

	@Override
	public Set<String> keySet() {
		return new HashSet<String>(Arrays.asList(keys()));
	}
	

	@Override
	public void putAll(Map<? extends String, ? extends T> map) {
		for (String key : map.keySet()) {
			put(key, map.get(key));
		}
	}
	

	@Override
	public int size() {
		return keys().length;
	}
	

	@Override
	public Collection<T> values() {
		List<T> values = new ArrayList<T>();
		
		for (String key : keys()) {
			values.add(get(key));
		}
		
		return values;
	}
	
	
	private class Entry implements Map.Entry<String, T> {
		
		private final String key;
		
		
		public Entry(String key) {
			this.key = key;
		}
		

		@Override
		public String getKey() {
			return key;
		}
		

		@Override
		public T getValue() {
			return get(key);
		}
		

		@Override
		public T setValue(T value) {
			return put(key, value);
		}
		
	}
	
	
	@SuppressWarnings("unchecked")
	public static <T> PreferencesMap<T> map(Preferences prefs, Class<T> type) {
		Adapter<T> adapter;
		
		if (type == String.class) {
			// prefer StringAdapter, because SimpleAdapter would use the copy constructor of String, instead of returning the values directly
			adapter = (Adapter<T>) new StringAdapter();
		} else {
			adapter = new SimpleAdapter(type);
		}
		
		return map(prefs, adapter);
	}
	

	public static <T> PreferencesMap<T> map(Preferences prefs, Adapter<T> adapter) {
		return new PreferencesMap<T>(prefs, adapter);
	}
	
	
	public static interface Adapter<T> {
		
		public T get(Preferences prefs, String key);
		

		public void put(Preferences prefs, String key, T value);
	}
	

	public static class StringAdapter implements Adapter<String> {
		
		@Override
		public String get(Preferences prefs, String key) {
			return prefs.get(key, null);
		}
		

		@Override
		public void put(Preferences prefs, String key, String value) {
			prefs.put(key, value);
		}
		
	}
	

	public static class SimpleAdapter<T> implements Adapter<T> {
		
		private final Constructor<T> constructor;
		
		
		public SimpleAdapter(Class<T> type) {
			try {
				constructor = type.getConstructor(String.class);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		

		@Override
		public T get(Preferences prefs, String key) {
			String stringValue = prefs.get(key, null);
			
			if (stringValue == null)
				return null;
			
			try {
				return constructor.newInstance(stringValue);
			} catch (InvocationTargetException e) {
				throw ExceptionUtil.asRuntimeException(e.getCause());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		

		@Override
		public void put(Preferences prefs, String key, T value) {
			prefs.put(key, value.toString());
		}
		
	}
	

	public static class SerializableAdapter<T extends Serializable> implements Adapter<T> {
		
		@SuppressWarnings("unchecked")
		@Override
		public T get(Preferences prefs, String key) {
			byte[] bytes = prefs.getByteArray(key, null);
			
			if (bytes == null)
				return null;
			
			try {
				ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
				Object object = in.readObject();
				in.close();
				
				return (T) object;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		

		@Override
		public void put(Preferences prefs, String key, T value) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			try {
				ObjectOutputStream out = new ObjectOutputStream(buffer);
				out.writeObject(value);
				out.close();
				
				prefs.putByteArray(key, buffer.toByteArray());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
