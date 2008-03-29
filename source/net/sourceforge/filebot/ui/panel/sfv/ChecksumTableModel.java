
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.filebot.FileFormat;


class ChecksumTableModel extends AbstractTableModel {
	
	private List<ChecksumRow> rows = new ArrayList<ChecksumRow>();
	private Map<String, ChecksumRow> rowMap = new HashMap<String, ChecksumRow>();
	
	private List<File> checksumColumnRoots = new ArrayList<File>();
	
	private final int checksumColumnsOffset = 2;
	
	
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex == 0)
			return "State";
		
		if (columnIndex == 1)
			return "Name";
		
		if (columnIndex >= checksumColumnsOffset) {
			File columnRoot = checksumColumnRoots.get(columnIndex - checksumColumnsOffset);
			return FileFormat.getFolderName(columnRoot);
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
		
		ChecksumComputationService.getService().purge();
	}
	

	public synchronized void clear() {
		ChecksumComputationService.getService().reset();
		
		checksumColumnRoots.clear();
		rows.clear();
		rowMap.clear();
		
		fireTableStructureChanged();
		fireTableDataChanged();
	}
	

	public File getChecksumColumnRoot(int checksumColumnIndex) {
		return checksumColumnRoots.get(checksumColumnIndex);
	}
	

	public Map<String, Checksum> getChecksumColumn(File columnRoot) {
		LinkedHashMap<String, Checksum> checksumMap = new LinkedHashMap<String, Checksum>();
		
		for (ChecksumRow row : rows) {
			Checksum checksum = row.getChecksum(columnRoot);
			
			if ((checksum != null) && (checksum.getState() == Checksum.State.READY)) {
				checksumMap.put(row.getName(), checksum);
			}
		}
		
		return checksumMap;
	}
	
	private final PropertyChangeListener checksumListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			fireTableChanged(new ChecksumTableModelEvent(ChecksumTableModel.this));
		}
	};
	
	
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
		

		@Override
		public String toString() {
			return getName();
		}
	}
	

	public static class ChecksumTableModelEvent extends TableModelEvent {
		
		public static final int CHECKSUM_PROGRESS = 10;
		
		
		public ChecksumTableModelEvent(TableModel source) {
			super(source);
			type = CHECKSUM_PROGRESS;
		}
		
	}
	
}
