
package net.sourceforge.filebot.ui.panel.analyze;


import static net.sourceforge.tuned.ui.LoadingOverlayPane.LOADING_PROPERTY;

import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.ui.panel.analyze.FileTree.FileNode;
import net.sourceforge.filebot.ui.panel.analyze.FileTree.FolderNode;
import net.sourceforge.tuned.ExceptionUtil;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.TunedUtilities;


abstract class Tool<M> extends JComponent {
	
	private UpdateModelTask updateTask = null;
	private Semaphore updateSemaphore = new Semaphore(1);
	
	
	public Tool(String name) {
		setName(name);
	}
	

	public synchronized void setSourceModel(FolderNode sourceModel) {
		if (updateTask != null) {
			updateTask.cancel(true);
		}
		
		updateTask = new UpdateModelTask(sourceModel);
		
		// sync events for loading overlay
		TunedUtilities.syncPropertyChangeEvents(boolean.class, LOADING_PROPERTY, updateTask, this);
		
		updateTask.execute();
	}
	

	protected abstract M createModelInBackground(FolderNode sourceModel) throws InterruptedException;
	

	protected abstract void setModel(M model);
	
	
	private class UpdateModelTask extends SwingWorker<M, Void> {
		
		private final FolderNode sourceModel;
		
		
		public UpdateModelTask(FolderNode sourceModel) {
			this.sourceModel = sourceModel;
		}
		

		@Override
		protected M doInBackground() throws Exception {
			// acquire semaphore
			updateSemaphore.acquireUninterruptibly();
			
			try {
				M model = null;
				
				if (!isCancelled()) {
					firePropertyChange(LOADING_PROPERTY, false, true);
					model = createModelInBackground(sourceModel);
					firePropertyChange(LOADING_PROPERTY, true, false);
				}
				
				return model;
			} finally {
				updateSemaphore.release();
			}
		}
		

		@Override
		protected void done() {
			// update task will only be cancelled if a newer update task has been started
			if (!isCancelled()) {
				try {
					setModel(get());
				} catch (Exception e) {
					if (ExceptionUtil.getRootCause(e) instanceof ConcurrentModificationException) {
						// if it happens, it is supposed to
					} else {
						// should not happen
						Logger.getLogger("global").log(Level.WARNING, e.toString(), e);
					}
				}
			}
		}
	}
	
	
	protected FolderNode createStatisticsNode(String name, List<File> files) {
		FolderNode folder = new FolderNode(null, files.size());
		
		long totalSize = 0;
		
		for (File file : files) {
			folder.add(new FileNode(file));
			totalSize += file.length();
		}
		
		// format the number of files string (e.g. 1 file, 2 files, ...)
		String numberOfFiles = String.format("%,d %s", files.size(), files.size() == 1 ? "file" : "files");
		
		// set node text (e.g. txt (1 file, 42 Byte))
		folder.setTitle(String.format("%s (%s, %s)", name, numberOfFiles, FileUtilities.formatSize(totalSize)));
		
		return folder;
	}
	
}
