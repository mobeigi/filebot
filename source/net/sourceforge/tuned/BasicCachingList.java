
package net.sourceforge.tuned;


import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;


public class BasicCachingList<E> extends AbstractList<E> {
	
	private final List<E> source;
	private final ArrayList<E> cache;
	
	
	public BasicCachingList(List<E> source) {
		this.source = source;
		
		int sourceSize = source.size();
		
		this.cache = new ArrayList<E>(sourceSize);
		
		// fill cache with null values
		for (int i = 0; i < sourceSize; i++) {
			cache.add(null);
		}
	}
	

	@Override
	public synchronized E get(int index) {
		E value = cache.get(index);
		
		if (value == null) {
			value = source.get(index);
			cache.set(index, value);
		}
		
		return value;
	}
	

	@Override
	public synchronized boolean add(E value) {
		cache.add(value);
		return source.add(value);
	}
	

	@Override
	public synchronized E remove(int index) {
		source.remove(index);
		return cache.remove(index);
	}
	

	@Override
	public synchronized int size() {
		return cache.size();
	}
	
}
