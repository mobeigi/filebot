
package net.sourceforge.filebot.ui.analyze;


import static net.sourceforge.filebot.ui.NotificationLogging.*;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import net.sourceforge.filebot.ui.analyze.FileTree.AbstractTreeNode;
import net.sourceforge.filebot.ui.analyze.FileTree.FileNode;
import net.sourceforge.filebot.ui.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.FastFile;
import net.sourceforge.tuned.FileUtilities;


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
		super.clear();
		
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
	protected void process(Exception e) {
		UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}
	
	
	@Override
	protected void load(List<File> files) {
		try {
			for (File file : files) {
				// use fast file to minimize system calls like length(), isDirectory(), isFile(), ...
				AbstractTreeNode node = getTreeNode(new FastFile(file.getPath()));
				
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
		
		File[] files = file.listFiles();
		if (files != null && file.isDirectory()) {
			FolderNode node = new FolderNode(FileUtilities.getFolderName(file), files.length);
			
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
