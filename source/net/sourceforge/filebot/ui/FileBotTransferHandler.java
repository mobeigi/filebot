
package net.sourceforge.filebot.ui;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import net.sourceforge.filebot.ui.sal.FileTransferable;
import net.sourceforge.filebot.ui.sal.Saveable;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicySupport;


public class FileBotTransferHandler extends TransferHandler {
	
	private TransferablePolicySupport transferablePolicySupport;
	
	private Saveable saveable;
	
	private boolean dragging;
	
	private String tmpdir = System.getProperty("java.io.tmpdir");
	
	
	public FileBotTransferHandler(TransferablePolicySupport transferablePolicySupport, Saveable saveable) {
		this.transferablePolicySupport = transferablePolicySupport;
		this.saveable = saveable;
	}
	
	private boolean canImportCache = false;
	
	
	@Override
	public boolean canImport(TransferSupport support) {
		// show "drop allowed" mousecursor when dragging even though drop is not allowed
		if (dragging)
			return true;
		
		if (transferablePolicySupport == null)
			return false;
		
		if (support.isDrop())
			support.setShowDropLocation(false);
		
		Transferable t = support.getTransferable();
		
		try {
			canImportCache = transferablePolicySupport.getTransferablePolicy().accept(t);
		} catch (InvalidDnDOperationException e) {
			// for some reason the last transferable has no drop current
		}
		
		return canImportCache;
	}
	

	@Override
	public boolean importData(TransferSupport support) {
		if (dragging)
			return false;
		
		if (!canImport(support))
			return false;
		
		boolean add = false;
		
		if (support.isDrop())
			add = support.getDropAction() == COPY;
		
		Transferable t = support.getTransferable();
		
		return transferablePolicySupport.getTransferablePolicy().handleTransferable(t, add);
	}
	

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		dragging = false;
		
		if (data == null)
			return;
		
		try {
			List<?> list = (List<?>) data.getTransferData(DataFlavor.javaFileListFlavor);
			
			for (Object object : list) {
				File temporaryFile = (File) object;
				temporaryFile.deleteOnExit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if (saveable == null || !saveable.isSaveable())
			return NONE;
		
		return MOVE | COPY;
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
				lines.add(path.getPathComponent(path.getPathCount() - 1).toString());
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
		
		StringBuffer b = new StringBuffer();
		Iterator<String> it = lines.iterator();
		
		while (it.hasNext()) {
			b.append(it.next());
			
			if (it.hasNext())
				b.append("\n");
		}
		
		clip.setContents(new StringSelection(b.toString()), null);
	}
	

	@Override
	protected Transferable createTransferable(JComponent c) {
		dragging = true;
		
		try {
			File temporaryFile = new File(tmpdir, saveable.getDefaultFileName());
			temporaryFile.createNewFile();
			
			saveable.save(temporaryFile);
			return new FileTransferable(temporaryFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
