
package net.sourceforge.filebot.format;


import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class AssociativeScriptObject implements GroovyObject {
	
	private final Map<String, Object> properties;
	

	public AssociativeScriptObject(Map<String, ?> properties) {
		this.properties = new LenientLookup(properties);
	}
	

	/**
	 * Get the property with the given name.
	 * 
	 * @param name the property name
	 * @param start the object where the lookup began
	 */
	@Override
	public Object getProperty(String name) {
		Object value = properties.get(name);
		
		if (value == null)
			throw new BindingException(name, "undefined");
		
		return value;
	}
	

	@Override
	public void setProperty(String name, Object value) {
		// ignore, object is immutable
	}
	

	@Override
	public Object invokeMethod(String name, Object args) {
		// ignore, object is merely a structure
		return null;
	}
	

	@Override
	public MetaClass getMetaClass() {
		return null;
	}
	

	@Override
	public void setMetaClass(MetaClass clazz) {
		// ignore, don't care about MetaClass
	}
	

	@Override
	public String toString() {
		// all the properties in alphabetic order
		return new TreeSet<String>(properties.keySet()).toString();
	}
	

	/**
	 * Map allowing look-up of values by a fault-tolerant key as specified by the defining key.
	 * 
	 */
	private static class LenientLookup extends AbstractMap<String, Object> {
		
		private final Map<String, Entry<String, ?>> lookup = new HashMap<String, Entry<String, ?>>();
		

		public LenientLookup(Map<String, ?> source) {
			// populate lookup map
			for (Entry<String, ?> entry : source.entrySet()) {
				lookup.put(definingKey(entry.getKey()), entry);
			}
		}
		

		protected String definingKey(Object key) {
			// letters and digits are defining, everything else will be ignored
			return key.toString().replaceAll("[^\\p{Alnum}]", "").toLowerCase();
		}
		

		@Override
		public boolean containsKey(Object key) {
			return lookup.containsKey(definingKey(key));
		}
		

		@Override
		public Object get(Object key) {
			Entry<String, ?> entry = lookup.get(definingKey(key));
			
			if (entry != null)
				return entry.getValue();
			
			return null;
		}
		

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return new AbstractSet<Entry<String, Object>>() {
				
				@Override
				public Iterator<Entry<String, Object>> iterator() {
					@SuppressWarnings("unchecked")
					Iterator<Entry<String, Object>> iterator = (Iterator) lookup.values().iterator();
					return iterator;
				}
				

				@Override
				public int size() {
					return lookup.size();
				}
			};
		}
	}
	
}
