
package net.sourceforge.tuned.ui;


import java.util.ArrayList;
import java.util.Collection;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;


public class ArrayListModel implements ListModel {
	
	private final ArrayList<Object> data;
	
	
	public ArrayListModel(Collection<? extends Object> data) {
		this.data = new ArrayList<Object>(data);
	}
	

	@Override
	public Object getElementAt(int index) {
		return data.get(index);
	}
	

	@Override
	public int getSize() {
		return data.size();
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
