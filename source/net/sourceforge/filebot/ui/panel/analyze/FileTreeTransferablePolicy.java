
package net.sourceforge.filebot.ui.panel.analyze;


import java.io.File;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;


class FileTreeTransferablePolicy extends BackgroundFileTransferablePolicy<DefaultMutableTreeNode> {
	
	private final FileTree tree;
	
	
	public FileTreeTransferablePolicy(FileTree tree) {
		this.tree = tree;
	}
	

	@Override
	protected void clear() {
		tree.clear();
	}
	

	@Override
	protected void process(List<DefaultMutableTreeNode> chunks) {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		
		for (DefaultMutableTreeNode node : chunks) {
			root.add(node);
		}
		
		model.reload(root);
	}
	

	@Override
	protected void load(List<File> files) {
		for (File file : files) {
			DefaultMutableTreeNode node = getTree(file);
			
			if (Thread.currentThread().isInterrupted())
				return;
			
			publish(node);
		}
	}
	

	private DefaultMutableTreeNode getTree(File file) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
		
		if (file.isDirectory() && !Thread.currentThread().isInterrupted()) {
			// run through folders first
			for (File f : file.listFiles(FileBotUtil.FOLDERS_ONLY)) {
				node.add(getTree(f));
			}
			
			// then files
			for (File f : file.listFiles(FileBotUtil.FILES_ONLY)) {
				node.add(getTree(f));
			}
		}
		
		return node;
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}
	
}
