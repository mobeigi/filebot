
package net.sourceforge.tuned.ui;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;


public class SimpleListModel extends AbstractListModel {
	
	private List<Object> list;
	
	
	public SimpleListModel() {
		list = Collections.synchronizedList(new ArrayList<Object>());
	}
	

	public SimpleListModel(Collection<? extends Object> collection) {
		list = Collections.synchronizedList(new ArrayList<Object>(collection));
	}
	

	@Override
	public Object getElementAt(int index) {
		return list.get(index);
	}
	

	@Override
	public int getSize() {
		return list.size();
	}
	

	public void add(Object object) {
		int index = list.size();
		list.add(object);
		fireIntervalAdded(this, index, index);
	}
	

	public void add(int index, Object object) {
		list.add(index, object);
		fireIntervalAdded(this, index, index);
	}
	

	public void addAll(Collection<? extends Object> c) {
		int begin = list.size();
		list.addAll(c);
		int end = list.size() - 1;
		
		if (end >= 0) {
			fireIntervalAdded(this, begin, end);
		}
	}
	

	public void clear() {
		int end = list.size() - 1;
		list.clear();
		
		if (end >= 0) {
			fireIntervalRemoved(this, 0, end);
		}
	}
	

	public boolean contains(Object o) {
		return list.contains(o);
	}
	

	public int indexOf(Object o) {
		return list.indexOf(o);
	}
	

	public boolean isEmpty() {
		return list.isEmpty();
	}
	

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}
	

	public Object remove(int index) {
		Object object = list.remove(index);
		
		fireIntervalRemoved(this, index, index);
		
		return object;
	}
	

	public void remove(Object object) {
		remove(indexOf(object));
	}
	

	public List<? extends Object> getCopy() {
		synchronized (list) {
			return new ArrayList<Object>(list);
		}
	}
	

	public void set(Collection<? extends Object> c) {
		int end = Math.max(list.size(), c.size()) - 1;
		
		list.clear();
		list.addAll(c);
		
		if (end >= 0) {
			fireContentsChanged(this, 0, end);
		}
	}
}
