package net.filebot.ui.filter;

import static net.filebot.Logging.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.swing.tree.TreeNode;

import org.apache.commons.io.FileUtils;

import net.filebot.ui.filter.FileTree.FileNode;
import net.filebot.ui.filter.FileTree.FolderNode;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.FileUtilities;
import net.filebot.util.ui.LoadingOverlayPane;

abstract class Tool<M> extends JComponent {

	private UpdateModelTask updateTask = null;
	private File root = null;

	public Tool(String name) {
		setName(name);
	}

	public File getRoot() {
		return root;
	}

	public void updateRoot(File root) {
		this.root = root;

		if (updateTask != null) {
			updateTask.cancel(true);
		}

		Tool.this.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, false, true);
		updateTask = new UpdateModelTask(root);
		updateTask.execute();
	}

	protected abstract M createModelInBackground(File root) throws InterruptedException;

	protected abstract void setModel(M model);

	private class UpdateModelTask extends SwingWorker<M, Void> {

		private final File root;

		public UpdateModelTask(File root) {
			this.root = root;
		}

		@Override
		protected M doInBackground() throws Exception {
			return createModelInBackground(root);
		}

		@Override
		protected void done() {
			if (this == updateTask) {
				Tool.this.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, true, false);
			}

			// update task will only be cancelled if a newer update task has been started
			if (this == updateTask && !isCancelled()) {
				try {
					setModel(get());
				} catch (Exception e) {
					Throwable cause = ExceptionUtilities.getRootCause(e);

					if (cause instanceof ConcurrentModificationException || cause instanceof InterruptedException) {
						// if it happens, it is supposed to
						debug.log(Level.FINEST, e.getMessage(), e);
					} else {
						// should not happen
						debug.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
		}
	}

	protected List<TreeNode> createFileNodes(Collection<File> files) {
		List<TreeNode> nodes = new ArrayList<TreeNode>(files.size());
		for (File f : files) {
			nodes.add(new FileNode(f));
		}
		return nodes;
	}

	protected FolderNode createStatisticsNode(String name, List<File> files) {
		long totalCount = 0;
		long totalSize = 0;

		for (File f : files) {
			totalCount += FileUtilities.listFiles(f).size();
			totalSize += FileUtils.sizeOf(f);
		}

		// set node text (e.g. txt (1 file, 42 Byte))
		String title = String.format("%s (%,d %s, %s)", name, totalCount, totalCount == 1 ? "file" : "files", FileUtilities.formatSize(totalSize));

		return new FolderNode(null, title, createFileNodes(files));
	}

}
