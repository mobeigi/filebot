
package net.sourceforge.filebot.ui.panel.analyze;


import java.io.File;
import java.util.List;

import net.sourceforge.filebot.ui.panel.analyze.FileTree.AbstractTreeNode;
import net.sourceforge.filebot.ui.panel.analyze.FileTree.FileNode;
import net.sourceforge.filebot.ui.panel.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.sourceforge.tuned.FileUtil;


class FileTreeTransferablePolicy extends BackgroundFileTransferablePolicy<AbstractTreeNode> {
	
	private final FileTree tree;
	
	
	public FileTreeTransferablePolicy(FileTree tree) {
		this.tree = tree;
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	protected void clear() {
		tree.clear();
	}
	

	@Override
	protected void process(List<AbstractTreeNode> chunks) {
		FolderNode root = tree.getRoot();
		
		for (AbstractTreeNode node : chunks) {
			root.add(node);
		}
		
		tree.getModel().reload();
	}
	

	@Override
	protected void load(List<File> files) {
		try {
			for (File file : files) {
				AbstractTreeNode node = getTreeNode(file);
				
				// publish on EDT
				publish(node);
			}
		} catch (InterruptedException e) {
			// supposed to happen if background execution was aborted
		}
	}
	

	private AbstractTreeNode getTreeNode(File file) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			
			FolderNode node = new FolderNode(FileUtil.getFolderName(file), files.length);
			
			// add folders first
			for (File f : files) {
				if (f.isDirectory()) {
					node.add(getTreeNode(f));
				}
			}
			
			for (File f : files) {
				if (f.isFile()) {
					node.add(getTreeNode(f));
				}
			}
			
			return node;
		}
		
		return new FileNode(file);
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}
	
}
