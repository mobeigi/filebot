
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.TreePath;


public class DefaultClipboardHandler implements ClipboardHandler {
	
	@Override
	public void exportToClipboard(JComponent component, Clipboard clip, int action) throws IllegalStateException {
		StringWriter buffer = new StringWriter();
		
		if (component instanceof JList) {
			export(new PrintWriter(buffer), (JList) component);
		} else if (component instanceof JTree) {
			export(new PrintWriter(buffer), (JTree) component);
		} else if (component instanceof JTable) {
			export(new PrintWriter(buffer), (JTable) component);
		}
		
		clip.setContents(new StringSelection(buffer.toString()), null);
	}
	

	protected void export(PrintWriter out, JList list) {
		for (Object value : list.getSelectedValues()) {
			out.println(valueToString(value));
		}
	}
	

	protected void export(PrintWriter out, JTree tree) {
		for (TreePath path : tree.getSelectionPaths()) {
			out.println(valueToString(path.getLastPathComponent()));
		}
	}
	

	protected void export(PrintWriter out, JTable table) {
		for (int row : table.getSelectedRows()) {
			for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
				out.print(valueToString(table.getModel().getValueAt(row, columnIndex)));
				
				if (columnIndex < table.getColumnCount() - 1)
					out.print("\t");
			}
			
			out.println();
		}
	}
	

	protected String valueToString(Object value) {
		// return empty string for null values
		if (value == null)
			return "";
		
		return value.toString();
	}
	
}
