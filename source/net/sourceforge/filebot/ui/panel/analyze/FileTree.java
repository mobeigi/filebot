
package net.sourceforge.filebot.ui.panel.analyze;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.ui.transfer.TransferablePolicyImportHandler;


public class FileTree extends FileBotTree {
	
	public static final String LOADING_PROPERTY = "loading";
	public static final String CONTENT_PROPERTY = "content";
	
	private PostProcessor postProcessor;
	
	
	public FileTree() {
		FileTreeTransferPolicy transferPolicy = new FileTreeTransferPolicy((DefaultMutableTreeNode) getModel().getRoot());
		transferPolicy.addPropertyChangeListener(LOADING_PROPERTY, new LoadingPropertyChangeListener());
		
		setTransferablePolicy(transferPolicy);
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
	

	@Override
	public void clear() {
		FileTreeTransferPolicy transferPolicy = ((FileTreeTransferPolicy) getTransferablePolicy());
		boolean loading = transferPolicy.isActive();
		
		if (loading) {
			transferPolicy.cancelAll();
		}
		
		super.clear();
		
		if (!loading) {
			contentChanged();
		}
		// else, contentChanged() will be called after when loading is finished
	}
	

	private void contentChanged() {
		synchronized (this) {
			if (postProcessor != null)
				postProcessor.cancel(false);
			
			postProcessor = new PostProcessor();
			postProcessor.execute();
		}
	};
	
	
	private class LoadingPropertyChangeListener implements PropertyChangeListener {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Boolean loading = (Boolean) evt.getNewValue();
			
			if (loading) {
				firePropertyChange(FileTree.LOADING_PROPERTY, null, true);
			} else {
				contentChanged();
			}
		}
	}
	

	private class PostProcessor extends SwingWorker<List<File>, Object> {
		
		@Override
		protected List<File> doInBackground() throws Exception {
			return convertToList();
		}
		

		@Override
		protected void done() {
			if (isCancelled())
				return;
			
			try {
				List<File> files = get();
				FileTree.this.firePropertyChange(CONTENT_PROPERTY, null, files);
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
			
			FileTree.this.firePropertyChange(FileTree.LOADING_PROPERTY, null, false);
			updateUI();
		}
		
	}
}
