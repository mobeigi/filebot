
package net.sourceforge.filebot.format;


import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.mozilla.javascript.Scriptable;


public class AssociativeScriptObject implements Scriptable {
	
	private final Map<String, Object> properties;
	

	@SuppressWarnings("unchecked")
	public AssociativeScriptObject(Map<String, ?> properties) {
		this.properties = new LenientLookup((Map<String, Object>) properties);
	}
	

	/**
	 * Defines properties available by name.
	 * 
	 * @param name the name of the property
	 * @param start the object where lookup began
	 */
	public boolean has(String name, Scriptable start) {
		return properties.containsKey(name);
	}
	

	/**
	 * Get the property with the given name.
	 * 
	 * @param name the property name
	 * @param start the object where the lookup began
	 */
	public Object get(String name, Scriptable start) {
		Object value = properties.get(name);
		
		if (value == null)
			throw new BindingException(name, "undefined");
		
		return value;
	}
	

	/**
	 * Defines properties available by index.
	 * 
	 * @param index the index of the property
	 * @param start the object where lookup began
	 */
	public boolean has(int index, Scriptable start) {
		// get property by index not supported
		return false;
	}
	

	/**
	 * Get property by index.
	 * 
	 * @param index the index of the property
	 * @param start the object where the lookup began
	 */
	public Object get(int index, Scriptable start) {
		// get property by index not supported
		throw new BindingException(String.valueOf(index), "undefined");
	}
	

	/**
	 * Get property names.
	 */
	public Object[] getIds() {
		return properties.keySet().toArray();
	}
	

	/**
	 * Returns the name of this JavaScript class.
	 */
	public String getClassName() {
		return getClass().getSimpleName();
	}
	

	/**
	 * Returns the string value of this object.
	 */
	@Override
	public Object getDefaultValue(Class<?> typeHint) {
		return this.toString();
	}
	

	@Override
	public String toString() {
		// all the properties in alphabetic order
		return new TreeSet<String>(properties.keySet()).toString();
	}
	

	public void put(String name, Scriptable start, Object value) {
		// ignore, object is immutable
	}
	

	public void put(int index, Scriptable start, Object value) {
		// ignore, object is immutable
	}
	

	public void delete(String id) {
		// ignore, object is immutable
	}
	

	public void delete(int index) {
		// ignore, object is immutable
	}
	

	public Scriptable getPrototype() {
		return null;
	}
	

	public void setPrototype(Scriptable prototype) {
		// ignore, don't care about prototype
	}
	

	public Scriptable getParentScope() {
		return null;
	}
	

	public void setParentScope(Scriptable parent) {
		// ignore, don't care about scope
	}
	

	public boolean hasInstance(Scriptable value) {
		return false;
	}
	

	/**
	 * Map allowing look-up of values by a fault-tolerant key as specified by the defining key.
	 * 
	 */
	protected class LenientLookup extends AbstractMap<String, Object> {
		
		private final Map<String, Entry<String, Object>> source;
		

		public LenientLookup(Map<String, Object> source) {
			// initialize source map
			this.source = new HashMap<String, Entry<String, Object>>(source.size());
			
			// populate source map
			for (Entry<String, Object> entry : source.entrySet()) {
				this.source.put(definingKey(entry.getKey()), entry);
			}
		}
		

		protected String definingKey(Object key) {
			// letters and digits are defining, everything else will be ignored
			return key.toString().replaceAll("[^\\p{Alnum}]", "").toLowerCase();
		}
		

		@Override
		public boolean containsKey(Object key) {
			return source.containsKey(definingKey(key));
		}
		

		@Override
		public Object get(Object key) {
			Entry<String, Object> entry = source.get(definingKey(key));
			
			if (entry != null)
				return entry.getValue();
			
			return null;
		}
		

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return new AbstractSet<Entry<String, Object>>() {
				
				@Override
				public Iterator<Entry<String, Object>> iterator() {
					return source.values().iterator();
				}
				

				@Override
				public int size() {
					return source.size();
				}
			};
		}
	}
	
}
