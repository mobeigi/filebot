
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.filebot.ui.FileFormat;


public class SfvTableModel extends AbstractTableModel {
	
	private ArrayList<ChecksumRow> rows = new ArrayList<ChecksumRow>();
	
	/**
	 * Used for Name->Checksum mapping (for performance reasons)
	 */
	private HashMap<String, ChecksumRow> rowMap = new HashMap<String, ChecksumRow>();
	
	private ArrayList<File> checksumColumnRoots = new ArrayList<File>();
	
	private int checksumColumnsOffset = 2;
	
	
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex == 0)
			return "State";
		
		if (columnIndex == 1)
			return "Name";
		
		if (columnIndex >= checksumColumnsOffset) {
			File columnRoot = checksumColumnRoots.get(columnIndex - checksumColumnsOffset);
			return FileFormat.getName(columnRoot);
		}
		
		return null;
	}
	

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0)
			return ChecksumRow.State.class;
		
		if (columnIndex == 1)
			return String.class;
		
		if (columnIndex >= checksumColumnsOffset)
			return Checksum.class;
		
		return null;
	}
	

	public int getColumnCount() {
		return checksumColumnsOffset + getChecksumColumnCount();
	}
	

	public int getChecksumColumnCount() {
		return checksumColumnRoots.size();
	}
	

	public int getRowCount() {
		return rows.size();
	}
	

	public Object getValueAt(int rowIndex, int columnIndex) {
		ChecksumRow row = rows.get(rowIndex);
		
		if (columnIndex == 0)
			return row.getState();
		
		if (columnIndex == 1)
			return row.getName();
		
		if (columnIndex >= checksumColumnsOffset) {
			File columnRoot = checksumColumnRoots.get(columnIndex - checksumColumnsOffset);
			return row.getChecksum(columnRoot);
		}
		
		return null;
	}
	

	public synchronized void addAll(List<Entry> list) {
		int firstRow = getRowCount();
		
		for (Entry entry : list) {
			addChecksum(entry.getName(), entry.getChecksum(), entry.getColumnRoot());
		}
		
		int lastRow = getRowCount() - 1;
		
		if (lastRow >= firstRow) {
			fireTableRowsInserted(firstRow, lastRow);
		}
	}
	

	private synchronized void addChecksum(String name, Checksum checksum, File columnRoot) {
		ChecksumRow row = rowMap.get(name);
		
		if (row == null) {
			row = new ChecksumRow(name);
			rows.add(row);
			rowMap.put(name, row);
		}
		
		row.putChecksum(columnRoot, checksum);
		checksum.addPropertyChangeListener(checksumListener);
		
		if (!checksumColumnRoots.contains(columnRoot)) {
			checksumColumnRoots.add(columnRoot);
			fireTableStructureChanged();
		}
	}
	

	public synchronized void removeRows(int... rowIndices) {
		ArrayList<ChecksumRow> rowsToRemove = new ArrayList<ChecksumRow>(rowIndices.length);
		
		for (int i : rowIndices) {
			ChecksumRow row = rows.get(i);
			rowsToRemove.add(rows.get(i));
			
			for (Checksum checksum : row.getChecksums()) {
				checksum.cancelComputationTask();
			}
		}
		
		rows.removeAll(rowsToRemove);
		fireTableRowsDeleted(rowIndices[0], rowIndices[rowIndices.length - 1]);
		
		ChecksumComputationExecutor.getInstance().purge();
	}
	

	public synchronized void clear() {
		
		// stop any running computations
		for (ChecksumRow row : rows) {
			for (Checksum checksum : row.getChecksums()) {
				checksum.cancelComputationTask();
			}
		}
		
		ChecksumComputationExecutor.getInstance().clear();
		
		checksumColumnRoots.clear();
		rows.clear();
		rowMap.clear();
		
		fireTableStructureChanged();
		fireTableDataChanged();
	}
	

	public File getChecksumColumnRoot(int checksumColumnIndex) {
		return checksumColumnRoots.get(checksumColumnIndex);
	}
	

	public LinkedHashMap<String, Checksum> getChecksumColumn(File columnRoot) {
		LinkedHashMap<String, Checksum> checksumMap = new LinkedHashMap<String, Checksum>();
		
		for (ChecksumRow row : rows) {
			Checksum checksum = row.getChecksum(columnRoot);
			
			if (checksum != null && checksum.getState() == Checksum.State.READY) {
				checksumMap.put(row.getName(), checksum);
			}
		}
		
		return checksumMap;
	}
	
	private PropertyChangeListener checksumListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			propertyChangeSupport.firePropertyChange(evt);
		}
	};
	
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	
	
	public static class Entry {
		
		private String name;
		
		private Checksum checksum;
		
		private File columnRoot;
		
		
		public Entry(String name, Checksum checksum, File columnRoot) {
			this.name = name;
			this.checksum = checksum;
			this.columnRoot = columnRoot;
		}
		

		public String getName() {
			return name;
		}
		

		public Checksum getChecksum() {
			return checksum;
		}
		

		public File getColumnRoot() {
			return columnRoot;
		}
	}
	
}
