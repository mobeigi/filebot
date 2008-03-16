
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.LinkedHashSet;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.ExportHandler;
import net.sourceforge.filebot.ui.transfer.FileTransferable;


class FileTreeExportHandler implements ExportHandler {
	
	@Override
	public Transferable createTransferable(JComponent c) {
		FileBotTree tree = (FileBotTree) c;
		
		LinkedHashSet<File> files = new LinkedHashSet<File>();
		
		for (TreePath path : tree.getSelectionPaths()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			
			files.addAll(tree.convertToList(node));
		}
		
		if (!files.isEmpty())
			return new FileTransferable(files);
		
		return null;
	}
	

	@Override
	public void exportDone(JComponent source, Transferable data, int action) {
		
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY;
	}
	
}
