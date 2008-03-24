
package net.sourceforge.filebot.ui.panel.analyze;


import java.io.File;
import java.io.FileFilter;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.filebot.ui.transferablepolicies.BackgroundFileTransferablePolicy;


class FileTreeTransferPolicy extends BackgroundFileTransferablePolicy<DefaultMutableTreeNode> {
	
	private FileTree tree;
	
	
	public FileTreeTransferPolicy(FileTree tree) {
		this.tree = tree;
	}
	

	@Override
	protected boolean accept(File file) {
		return file.isFile() || file.isDirectory();
	}
	

	@Override
	protected void clear() {
		tree.clear();
	}
	

	@Override
	protected void process(List<DefaultMutableTreeNode> chunks) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		
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
			// run through folders first
			for (File f : file.listFiles(FOLDER_FILTER)) {
				node.add(getTree(f));
			}
			
			// then files
			for (File f : file.listFiles(FILE_FILTER)) {
				node.add(getTree(f));
			}
		}
		
		return node;
	}
	

	@Override
	public String getDescription() {
		return "files and folders";
	}
	
	private static final FileFilter FOLDER_FILTER = new FileFilter() {
		
		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
		
	};
	
	private static final FileFilter FILE_FILTER = new FileFilter() {
		
		@Override
		public boolean accept(File file) {
			return file.isFile();
		}
		
	};
	
}
