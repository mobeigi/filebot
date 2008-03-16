
package net.sourceforge.filebot.ui.panel.analyze;


import java.io.File;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.filebot.ui.transferablepolicies.BackgroundFileTransferablePolicy;


class FileTreeTransferPolicy extends BackgroundFileTransferablePolicy<DefaultMutableTreeNode> {
	
	DefaultMutableTreeNode root;
	
	
	public FileTreeTransferPolicy(DefaultMutableTreeNode root) {
		this.root = root;
	}
	

	@Override
	protected boolean accept(File file) {
		return file.isFile() || file.isDirectory();
	}
	

	@Override
	protected void clear() {
		root.removeAllChildren();
	}
	

	@Override
	protected void process(List<DefaultMutableTreeNode> chunks) {
		for (DefaultMutableTreeNode node : chunks) {
			root.add(node);
		}
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
	

	@Override
	public String getDescription() {
		return "files and folders";
	}
	
}
