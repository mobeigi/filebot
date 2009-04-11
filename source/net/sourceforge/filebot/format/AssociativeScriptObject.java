
package net.sourceforge.filebot.format;


import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.Scriptable;


class AssociativeScriptObject implements Scriptable {
	
	/**
	 * Map allowing look-up of values by a fault-tolerant key as specified by the defining key.
	 * 
	 * @see {@link #definingKey(String)}
	 */
	protected final TreeMap<String, Object> properties = new TreeMap<String, Object>(new Comparator<String>() {
		
		@Override
		public int compare(String s1, String s2) {
			return definingKey(s1).compareTo(definingKey(s2));
		}
	});
	
	
	/**
	 * The Java constructor
	 */
	public AssociativeScriptObject(Map<String, ?> properties) {
		this.properties.putAll(properties);
	}
	

	protected String definingKey(String s) {
		// letters and digits are defining, everything else will be ignored
		return s.replaceAll("\\p{Punct}", "").toLowerCase();
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
	@SuppressWarnings("unchecked")
	@Override
	public Object getDefaultValue(Class typeHint) {
		return this.toString();
	}
	

	@Override
	public String toString() {
		return getClassName() + properties.entrySet().toString();
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
	
}
