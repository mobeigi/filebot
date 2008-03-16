
package net.sourceforge.filebot.ui.panel.analyze;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.ui.transfer.TransferablePolicyImportHandler;
import net.sourceforge.filebot.ui.transferablepolicies.BackgroundFileTransferablePolicy;


public class FileTree extends FileBotTree {
	
	public static final String LOADING_PROPERTY = "loading";
	public static final String CONTENT_PROPERTY = "content";
	
	
	public FileTree() {
		setTransferablePolicy(new FileTreeTransferPolicy());
		setTransferHandler(new DefaultTransferHandler(new TransferablePolicyImportHandler(this), null));
	}
	

	public void removeTreeItems(TreePath paths[]) {
		firePropertyChange(LOADING_PROPERTY, null, true);
		
		for (TreePath element : paths) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) (element.getLastPathComponent());
			node.removeFromParent();
		}
		
		contentChanged();
		firePropertyChange(LOADING_PROPERTY, null, false);
	}
	

	public void load(List<File> files) {
		FileTransferable tr = new FileTransferable(files);
		
		if (getTransferablePolicy().accept(tr))
			getTransferablePolicy().handleTransferable(tr, true);
	}
	

	private void contentChanged() {
		List<File> files = convertToList();
		firePropertyChange(CONTENT_PROPERTY, null, files);
	}
	

	@Override
	public void clear() {
		((BackgroundFileTransferablePolicy<?>) getTransferablePolicy()).cancelAll();
		
		super.clear();
		contentChanged();
	}
	
	
	private class FileTreeTransferPolicy extends BackgroundFileTransferablePolicy<DefaultMutableTreeNode> implements PropertyChangeListener {
		
		public FileTreeTransferPolicy() {
			addPropertyChangeListener(LOADING_PROPERTY, this);
		}
		

		@Override
		protected boolean accept(File file) {
			return file.isFile() || file.isDirectory();
		}
		

		@Override
		protected void clear() {
			FileTree.this.clear();
		}
		

		@Override
		protected void process(List<DefaultMutableTreeNode> chunks) {
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
			
			for (DefaultMutableTreeNode node : chunks) {
				root.add(node);
			}
			
			updateUI();
		}
		

		@Override
		protected void load(List<File> files) {
			for (File file : files) {
				publish(getTree(file));
			}
		}
		

		private DefaultMutableTreeNode getTree(File file) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
			
			if (file.isDirectory() && !Thread.currentThread().isInterrupted()) {
				// run through file tree
				for (File f : file.listFiles()) {
					node.add(getTree(f));
				}
			}
			
			return node;
		}
		

		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName() == BackgroundFileTransferablePolicy.LOADING_PROPERTY) {
				Boolean loading = (Boolean) evt.getNewValue();
				
				if (loading) {
					FileTree.this.firePropertyChange(FileTree.LOADING_PROPERTY, null, true);
				} else {
					FileTree.this.firePropertyChange(FileTree.LOADING_PROPERTY, null, false);
					
					contentChanged();
				}
			}
		}
		

		@Override
		public String getDescription() {
			return "files and folders";
		}
		
	}
	
}
