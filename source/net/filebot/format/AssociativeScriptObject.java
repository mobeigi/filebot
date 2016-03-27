
package net.filebot.format;


import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import groovy.lang.GroovyObjectSupport;


public class AssociativeScriptObject extends GroovyObjectSupport implements Iterable<Entry<Object, Object>> {

	private final Map<Object, Object> properties;


	public AssociativeScriptObject(Map<?, ?> properties) {
		this.properties = new LenientLookup(properties);
	}


	/**
	 * Get the property with the given name.
	 *
	 * @param name
	 *            the property name
	 * @param start
	 *            the object where the lookup began
	 */
	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}


	@Override
	public void setProperty(String name, Object value) {
		// ignore, object is immutable
	}


	@Override
	public Iterator<Entry<Object, Object>> iterator() {
		return properties.entrySet().iterator();
	}


	@Override
	public String toString() {
		// all the properties in alphabetic order
		return new TreeSet<Object>(properties.keySet()).toString();
	}


	/**
	 * Map allowing look-up of values by a fault-tolerant key as specified by the defining key.
	 *
	 */
	private static class LenientLookup extends AbstractMap<Object, Object> {

		private final Map<String, Entry<?, ?>> lookup = new HashMap<String, Entry<?, ?>>();


		public LenientLookup(Map<?, ?> source) {
			// populate lookup map
			for (Entry<?, ?> entry : source.entrySet()) {
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
			Entry<?, ?> entry = lookup.get(definingKey(key));

			if (entry != null)
				return entry.getValue();

			return null;
		}


		@Override
		public Set<Entry<Object, Object>> entrySet() {
			return new AbstractSet<Entry<Object, Object>>() {

				@Override
				public Iterator<Entry<Object, Object>> iterator() {
					return (Iterator) lookup.values().iterator();
				}


				@Override
				public int size() {
					return lookup.size();
				}
			};
		}
	}

}
