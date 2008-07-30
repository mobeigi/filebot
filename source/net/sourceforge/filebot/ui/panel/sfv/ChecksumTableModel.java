
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.tuned.FileUtil;


class ChecksumTableModel extends AbstractTableModel {
	
	private List<ChecksumRow> rows = new ArrayList<ChecksumRow>(50);
	
	/**
	 * Hash map for fast access to the row of a given name
	 */
	private Map<String, ChecksumRow> rowMap = new HashMap<String, ChecksumRow>(50);
	
	private List<File> columns = new ArrayList<File>();
	
	/**
	 * Checksum start at column 3
	 */
	private static final int checksumColumnOffset = 2;
	
	
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex == 0)
			return "State";
		
		if (columnIndex == 1)
			return "Name";
		
		if (columnIndex >= checksumColumnOffset) {
			File column = columns.get(columnIndex - checksumColumnOffset);
			
			// works for files too and simply returns the name unchanged
			return FileUtil.getFolderName(column);
		}
		
		return null;
	}
	

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0)
			return ChecksumRow.State.class;
		
		if (columnIndex == 1)
			return String.class;
		
		if (columnIndex >= checksumColumnOffset)
			return Checksum.class;
		
		return null;
	}
	

	public int getColumnCount() {
		return checksumColumnOffset + getChecksumColumnCount();
	}
	

	public int getChecksumColumnCount() {
		return columns.size();
	}
	

	public List<File> getChecksumColumns() {
		return Collections.unmodifiableList(columns);
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
		
		if (columnIndex >= checksumColumnOffset) {
			File column = columns.get(columnIndex - checksumColumnOffset);
			return row.getChecksum(column);
		}
		
		return null;
	}
	

	public void addAll(List<ChecksumCell> list) {
		int firstRow = getRowCount();
		
		for (ChecksumCell entry : list) {
			addChecksum(entry.getName(), entry.getChecksum(), entry.getColumn());
		}
		
		int lastRow = getRowCount() - 1;
		
		if (lastRow >= firstRow) {
			fireTableRowsInserted(firstRow, lastRow);
		}
	}
	

	private void addChecksum(String name, Checksum checksum, File column) {
		ChecksumRow row = rowMap.get(name);
		
		if (row == null) {
			row = new ChecksumRow(name);
			rows.add(row);
			rowMap.put(name, row);
		}
		
		row.putChecksum(column, checksum);
		checksum.addPropertyChangeListener(checksumListener);
		
		if (!columns.contains(column)) {
			columns.add(column);
			fireTableStructureChanged();
		}
	}
	

	public void removeRows(int... rowIndices) {
		ArrayList<ChecksumRow> rowsToRemove = new ArrayList<ChecksumRow>(rowIndices.length);
		
		for (int i : rowIndices) {
			ChecksumRow row = rows.get(i);
			rowsToRemove.add(rows.get(i));
			
			for (Checksum checksum : row.getChecksums()) {
				checksum.cancelComputationTask();
			}
			
			rowMap.remove(row.getName());
		}
		
		rows.removeAll(rowsToRemove);
		fireTableRowsDeleted(rowIndices[0], rowIndices[rowIndices.length - 1]);
	}
	

	public void clear() {
		columns.clear();
		rows.clear();
		rowMap.clear();
		fireTableStructureChanged();
		
		fireTableDataChanged();
	}
	

	public File getChecksumColumn(int columnIndex) {
		return columns.get(columnIndex);
	}
	

	public Map<String, Checksum> getChecksumColumn(File column) {
		LinkedHashMap<String, Checksum> checksumMap = new LinkedHashMap<String, Checksum>();
		
		for (ChecksumRow row : rows) {
			Checksum checksum = row.getChecksum(column);
			
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
	
	
	public static class ChecksumCell {
		
		private final String name;
		private final Checksum checksum;
		private final File column;
		
		
		public ChecksumCell(String name, Checksum checksum, File column) {
			this.name = name;
			this.checksum = checksum;
			this.column = column;
		}
		

		public String getName() {
			return name;
		}
		

		public Checksum getChecksum() {
			return checksum;
		}
		

		public File getColumn() {
			return column;
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
