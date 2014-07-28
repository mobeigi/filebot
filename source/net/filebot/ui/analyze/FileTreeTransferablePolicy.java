package net.filebot.ui.analyze;

import static net.filebot.ui.NotificationLogging.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.tree.TreeNode;

import net.filebot.ui.analyze.FileTree.FileNode;
import net.filebot.ui.analyze.FileTree.FolderNode;
import net.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.FastFile;

class FileTreeTransferablePolicy extends BackgroundFileTransferablePolicy<TreeNode> {

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
	protected void process(List<TreeNode> root) {
		tree.getModel().setRoot(root.get(0));
		tree.getModel().reload();
	}

	@Override
	protected void process(Exception e) {
		UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}

	@Override
	protected void load(List<File> files) {
		try {
			if (files.size() > 1 || containsOnly(files, FILES)) {
				files = Arrays.asList(files.get(0).getParentFile());
			}

			// use fast file to minimize system calls like length(), isDirectory(), isFile(), ...
			FastFile root = FastFile.create(filter(files, FOLDERS)).get(0);

			// publish on EDT
			publish(getTreeNode(root));
		} catch (InterruptedException e) {
			// supposed to happen if background execution was aborted
		}
	}

	private TreeNode getTreeNode(File file) throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

		if (file.isDirectory()) {
			LinkedList<TreeNode> children = new LinkedList<TreeNode>();
			for (File f : getChildren(file)) {
				if (f.isHidden())
					continue;

				if (f.isDirectory()) {
					children.addFirst(getTreeNode(f));
				} else {
					children.addLast(getTreeNode(f));
				}
			}

			return new FolderNode(file, getFolderName(file), new ArrayList<TreeNode>(children));
		}

		return new FileNode(file);
	}

	@Override
	public String getFileFilterDescription() {
		return "Folders";
	}

}
