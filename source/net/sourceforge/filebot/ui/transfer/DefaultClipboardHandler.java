
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.TreePath;


public class DefaultClipboardHandler implements ClipboardHandler {
	
	protected final String newLine = System.getProperty("line.separator");
	
	
	@Override
	public void exportToClipboard(JComponent component, Clipboard clip, int action) throws IllegalStateException {
		StringBuilder sb = new StringBuilder();
		
		if (component instanceof JList) {
			export(sb, (JList) component);
		} else if (component instanceof JTree) {
			export(sb, (JTree) component);
		} else if (component instanceof JTable) {
			export(sb, (JTable) component);
		}
		
		clip.setContents(new StringSelection(sb.toString()), null);
	}
	

	protected void export(StringBuilder sb, JList list) {
		for (Object value : list.getSelectedValues()) {
			sb.append(value == null ? "" : value).append(newLine);
		}
		
		// delete last newline
		sb.delete(sb.length() - newLine.length(), sb.length());
	}
	

	protected void export(StringBuilder sb, JTree tree) {
		for (TreePath path : tree.getSelectionPaths()) {
			Object value = path.getLastPathComponent();
			
			sb.append(value == null ? "" : value).append(newLine);
		}
		
		// delete last newline
		sb.delete(sb.length() - newLine.length(), sb.length());
	}
	

	protected void export(StringBuilder sb, JTable table) {
		for (int row : table.getSelectedRows()) {
			int modelRow = table.getRowSorter().convertRowIndexToModel(row);
			
			for (int column = 0; column < table.getColumnCount(); column++) {
				Object value = table.getModel().getValueAt(modelRow, column);
				
				if (value != null) {
					sb.append(value);
				}
				
				if (column < table.getColumnCount() - 1) {
					sb.append("\t");
				}
			}
			
			sb.append(newLine);
		}
		
		// delete last newline
		sb.delete(sb.length() - newLine.length(), sb.length());
	}
}
