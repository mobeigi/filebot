
package net.sourceforge.tuned.ui;


import java.util.Arrays;
import java.util.Collection;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;


public class ArrayListModel implements ListModel {
	
	private final Object[] data;
	
	
	public ArrayListModel(Collection<? extends Object> data) {
		this.data = data.toArray();
	}
	

	public ArrayListModel(Object[] data) {
		this.data = Arrays.copyOf(data, data.length);
	}
	

	@Override
	public Object getElementAt(int index) {
		return data[index];
	}
	

	@Override
	public int getSize() {
		return data.length;
	}
	

	@Override
	public void addListDataListener(ListDataListener l) {
		// ignore, model is unmodifiable
	}
	

	@Override
	public void removeListDataListener(ListDataListener l) {
		// ignore, model is unmodifiable
	}
	
}
