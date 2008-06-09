
package net.sourceforge.tuned;


import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import net.sourceforge.tuned.PreferencesMap.Adapter;


public class PreferencesList<T> extends AbstractList<T> {
	
	private final PreferencesMap<T> prefs;
	
	
	public PreferencesList(PreferencesMap<T> preferencesMap) {
		this.prefs = preferencesMap;
	}
	

	@Override
	public T get(int index) {
		return prefs.get(key(index));
	}
	

	private String key(int index) {
		return Integer.toString(index);
	}
	

	@Override
	public int size() {
		return prefs.size();
	}
	

	@Override
	public boolean add(T e) {
		prefs.put(key(size()), e);
		return true;
	}
	

	@Override
	public T remove(int index) {
		
		int lastIndex = size() - 1;
		
		List<T> shiftList = new ArrayList<T>(subList(index, lastIndex + 1));
		
		T value = shiftList.remove(0);
		
		prefs.remove(key(lastIndex));
		
		for (T element : shiftList) {
			set(index, element);
			index++;
		}
		
		return value;
	}
	

	@Override
	public T set(int index, T element) {
		if (index < 0 || index >= size())
			throw new IndexOutOfBoundsException();
		
		return prefs.put(key(index), element);
	}
	

	@Override
	public void clear() {
		prefs.clear();
	}
	

	public void set(List<T> data) {
		clear();
		addAll(data);
	}
	

	public static <T> PreferencesList<T> map(Preferences prefs, Class<T> type) {
		return new PreferencesList<T>(PreferencesMap.map(prefs, type));
	}
	

	public static <T> PreferencesList<T> map(Preferences prefs, Adapter<T> adapter) {
		return new PreferencesList<T>(PreferencesMap.map(prefs, adapter));
	}
}
