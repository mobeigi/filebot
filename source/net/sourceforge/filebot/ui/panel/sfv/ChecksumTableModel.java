
package net.sourceforge.filebot.ui.panel.sfv;


import static javax.swing.event.TableModelEvent.UPDATE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.tuned.FileUtilities;


class ChecksumTableModel extends AbstractTableModel implements Iterable<ChecksumRow> {
	
	private final IndexedMap<String, ChecksumRow> rows = new IndexedMap<String, ChecksumRow>() {
		
		@Override
		public String key(ChecksumRow value) {
			return value.getName();
		}
	};
	
	private final List<File> columns = new ArrayList<File>();
	
	
	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
			case 0:
				return "State";
			case 1:
				return "Name";
			default:
				// works for files too and simply returns the name unchanged
				return FileUtilities.getFolderName(getColumnRoot(columnIndex));
		}
	}
	

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case 0:
				return ChecksumRow.State.class;
			case 1:
				return String.class;
			default:
				return ChecksumCell.class;
		}
	}
	

	public File getColumnRoot(int columnIndex) {
		return columns.get(columnIndex - 2);
	}
	

	@Override
	public int getColumnCount() {
		return columns.size() + 2;
	}
	

	public List<File> getChecksumList() {
		return Collections.unmodifiableList(columns);
	}
	

	@Override
	public int getRowCount() {
		return rows.size();
	}
	

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		ChecksumRow row = rows.get(rowIndex);
		
		switch (columnIndex) {
			case 0:
				return row.getState();
			case 1:
				return row.getName();
			default:
				return row.getChecksum(getColumnRoot(columnIndex));
		}
	}
	

	@Override
	public Iterator<ChecksumRow> iterator() {
		return rows.iterator();
	}
	

	public void addAll(List<ChecksumCell> list) {
		int firstRow = getRowCount();
		
		for (ChecksumCell entry : list) {
			ChecksumRow row = rows.getByKey(entry.getName());
			
			if (row == null) {
				row = new ChecksumRow(entry.getName());
				rows.add(row);
			}
			
			row.add(entry);
			
			// listen to changes (progress, state)
			entry.addPropertyChangeListener(progressListener);
			
			if (!columns.contains(entry.getRoot())) {
				columns.add(entry.getRoot());
				fireTableStructureChanged();
			}
		}
		
		int lastRow = getRowCount() - 1;
		
		if (lastRow >= firstRow) {
			fireTableRowsInserted(firstRow, lastRow);
		}
	}
	

	public void remove(int... index) {
		for (int i : index) {
			for (ChecksumCell entry : rows.get(i).values()) {
				entry.removePropertyChangeListener(progressListener);
				entry.dispose();
			}
		}
		
		// remove rows
		rows.removeAll(index);
		
		fireTableRowsDeleted(index[0], index[index.length - 1]);
	}
	

	public void clear() {
		columns.clear();
		rows.clear();
		
		fireTableStructureChanged();
	}
	
	private final PropertyChangeListener progressListener = new PropertyChangeListener() {
		
		private final MutableTableModelEvent mutableUpdateEvent = new MutableTableModelEvent(ChecksumTableModel.this, UPDATE);
		
		
		public void propertyChange(PropertyChangeEvent evt) {
			ChecksumCell entry = (ChecksumCell) evt.getSource();
			
			int index = rows.getIndexByKey(entry.getName());
			
			if (index >= 0) {
				rows.get(index).updateState();
				fireTableChanged(mutableUpdateEvent.setRow(index));
			}
		}
	};
	
	
	protected static class MutableTableModelEvent extends TableModelEvent {
		
		public MutableTableModelEvent(TableModel source, int type) {
			super(source, 0, 0, ALL_COLUMNS, type);
		}
		

		public MutableTableModelEvent setRow(int row) {
			this.firstRow = row;
			this.lastRow = row;
			
			return this;
		}
	}
	

	protected static abstract class IndexedMap<K, V> extends AbstractList<V> implements Set<V> {
		
		private final Map<K, Integer> indexMap = new HashMap<K, Integer>(64);
		private final List<V> list = new ArrayList<V>(64);
		
		
		public abstract K key(V value);
		

		@Override
		public V get(int index) {
			return list.get(index);
		}
		

		public V getByKey(K key) {
			Integer index = indexMap.get(key);
			
			if (index == null)
				return null;
			
			return get(index);
		}
		

		public int getIndexByKey(K key) {
			Integer index = indexMap.get(key);
			
			if (index == null)
				return -1;
			
			return index;
		}
		

		@Override
		public boolean add(V value) {
			K key = key(value);
			Integer index = indexMap.get(key);
			
			if (index == null && list.add(value)) {
				indexMap.put(key, lastIndexOf(value));
				return true;
			}
			
			return false;
		}
		

		public void removeAll(int... index) {
			// sort index array
			Arrays.sort(index);
			
			// remove in reverse
			for (int i = index.length - 1; i >= 0; i--) {
				V value = list.remove(index[i]);
				indexMap.remove(key(value));
			}
			
			updateIndexMap();
		}
		

		private void updateIndexMap() {
			for (int i = 0; i < list.size(); i++) {
				indexMap.put(key(list.get(i)), i);
			}
		}
		

		@Override
		public int size() {
			return list.size();
		}
		

		@Override
		public void clear() {
			list.clear();
			indexMap.clear();
		}
		
	}
	
}
