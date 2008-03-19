
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.TreePath;


public class DefaultClipboardHandler implements ClipboardHandler {
	
	@Override
	public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
		ArrayList<String> lines = new ArrayList<String>();
		
		if (comp instanceof JList) {
			JList list = (JList) comp;
			for (Object value : list.getSelectedValues()) {
				lines.add(value.toString());
			}
		} else if (comp instanceof JTree) {
			JTree tree = (JTree) comp;
			for (TreePath path : tree.getSelectionPaths()) {
				lines.add(path.getLastPathComponent().toString());
			}
		} else if (comp instanceof JTable) {
			JTable table = (JTable) comp;
			
			for (int row : table.getSelectedRows()) {
				StringBuilder sb = new StringBuilder();
				int maxCol = table.getColumnCount() - 1;
				for (int col = 0; col <= maxCol; col++) {
					sb.append(table.getModel().getValueAt(row, col));
					
					if (col != maxCol)
						sb.append("\t");
				}
				
				lines.add(sb.toString());
			}
		}
		
		StringBuilder buffer = new StringBuilder();
		Iterator<String> it = lines.iterator();
		
		while (it.hasNext()) {
			buffer.append(it.next());
			
			if (it.hasNext())
				buffer.append("\n");
		}
		
		clip.setContents(new StringSelection(buffer.toString()), null);
	}
	
}
