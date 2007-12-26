
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
	

	public void addFiles(DefaultMutableTreeNode parent, File files[]) {
		// folders first
		for (File f : files)
			if (f.isDirectory()) {
				// run through file tree
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(f);
				addFiles(node, f.listFiles());
				parent.add(node);
			}
		
		for (File f : files) {
			if (!f.isDirectory())
				parent.add(new DefaultMutableTreeNode(f));
		}
	}
	

	@Override
	public void clear() {
		super.clear();
		contentChanged();
	}
	
	
	private class FileTreeTransferPolicy extends BackgroundFileTransferablePolicy<Object> implements PropertyChangeListener {
		
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
		protected boolean load(List<File> files) {
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
			
			File fileArray[] = new File[files.size()];
			files.toArray(fileArray);
			
			addFiles(root, fileArray);
			
			return true;
		}
		

		/**
		 * This method will not be used
		 */
		@Override
		protected boolean load(File file) {
			return false;
		}
		

		public void propertyChange(PropertyChangeEvent evt) {
			Boolean loading = (Boolean) evt.getNewValue();
			
			if (loading) {
				FileTree.this.firePropertyChange(FileTree.LOADING_PROPERTY, null, true);
			} else {
				FileTree.this.firePropertyChange(FileTree.LOADING_PROPERTY, null, false);
				updateUI();
				contentChanged();
			}
		}
		

		@Override
		public String getDescription() {
			return "files and folders";
		}
		
	}
	
}
