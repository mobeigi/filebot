
package net.sourceforge.filebot.ui.panel.analyze;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.ui.transfer.TransferablePolicyImportHandler;


class FileTree extends FileBotTree {
	
	public static final String LOADING_PROPERTY = "loading";
	public static final String CONTENT_PROPERTY = "content";
	
	private PostProcessor postProcessor;
	
	
	public FileTree() {
		FileTreeTransferablePolicy transferPolicy = new FileTreeTransferablePolicy(this);
		transferPolicy.addPropertyChangeListener(LOADING_PROPERTY, new LoadingPropertyChangeListener());
		
		setTransferablePolicy(transferPolicy);
		setTransferHandler(new DefaultTransferHandler(new TransferablePolicyImportHandler(this), null));
	}
	

	public void removeTreeItems(TreePath paths[]) {
		List<TreeNode> changedNodes = new ArrayList<TreeNode>(paths.length);
		
		for (TreePath element : paths) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) (element.getLastPathComponent());
			
			if (!node.isRoot()) {
				changedNodes.add(node.getParent());
				node.removeFromParent();
			}
		}
		
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		
		for (TreeNode treeNode : changedNodes) {
			model.reload(treeNode);
		}
		
		contentChanged();
	}
	

	public void load(List<File> files) {
		FileTransferable tr = new FileTransferable(files);
		
		if (getTransferablePolicy().accept(tr))
			getTransferablePolicy().handleTransferable(tr, true);
	}
	

	@Override
	public void clear() {
		((FileTreeTransferablePolicy) getTransferablePolicy()).reset();
		
		// there may still be some runnables from the transfer in the event queue, 
		// clear the model, after those runnables have finished,
		// otherwise it may happen, that stuff is added, after the model has been cleared
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				FileTree.super.clear();
				contentChanged();
			}
		});
		
	}
	

	void contentChanged() {
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
			
			firePropertyChange(FileTree.LOADING_PROPERTY, null, loading);
			
			if (!loading) {
				((DefaultTreeModel) getModel()).reload();
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
		}
		
	}
}
