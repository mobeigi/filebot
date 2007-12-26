
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;


public class DefaultTransferHandler extends TransferHandler {
	
	private ImportHandler importHandler;
	private ExportHandler exportHandler;
	
	private boolean dragging = false;
	
	
	public DefaultTransferHandler(ImportHandler importHandler, ExportHandler exportHandler) {
		this.importHandler = importHandler;
		this.exportHandler = exportHandler;
	}
	

	@Override
	public boolean canImport(TransferSupport support) {
		// show "drop allowed" cursor when dragging even though drop is not allowed
		if (dragging)
			return true;
		
		if (importHandler != null)
			return importHandler.canImport(support);
		
		return false;
	}
	

	@Override
	public boolean importData(TransferSupport support) {
		if (dragging)
			return false;
		
		if (!canImport(support))
			return false;
		
		return importHandler.importData(support);
	}
	

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		dragging = false;
		
		if (data == null)
			return;
		
		if (exportHandler != null)
			exportHandler.exportDone(source, data, action);
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if (exportHandler != null)
			return exportHandler.getSourceActions(c);
		
		return NONE;
	}
	

	@Override
	protected Transferable createTransferable(JComponent c) {
		dragging = true;
		
		if (exportHandler != null)
			return exportHandler.createTransferable(c);
		
		return null;
	}
	

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
				StringBuffer b = new StringBuffer();
				int maxCol = table.getColumnCount() - 1;
				for (int col = 0; col <= maxCol; col++) {
					b.append(table.getModel().getValueAt(row, col));
					
					if (col != maxCol)
						b.append("\t");
				}
				
				lines.add(b.toString());
			}
		}
		
		StringBuffer buffer = new StringBuffer();
		Iterator<String> it = lines.iterator();
		
		while (it.hasNext()) {
			buffer.append(it.next());
			
			if (it.hasNext())
				buffer.append("\n");
		}
		
		clip.setContents(new StringSelection(buffer.toString()), null);
	}
	
}
