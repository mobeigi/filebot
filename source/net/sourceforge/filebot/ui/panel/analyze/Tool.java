
package net.sourceforge.filebot.ui.panel.analyze;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.ui.panel.analyze.FileTree.FileNode;
import net.sourceforge.filebot.ui.panel.analyze.FileTree.FolderNode;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.ui.LoadingOverlayPane;


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
		updateTask.addPropertyChangeListener(loadingListener);
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
					firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, false, true);
					model = createModelInBackground(sourceModel);
					firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, true, false);
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
					// should not happen
					Logger.getLogger("global").log(Level.WARNING, e.toString());
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
		folder.setTitle(String.format("%s (%s, %s)", name, numberOfFiles, FileUtil.formatSize(totalSize)));
		
		return folder;
	}
	
	private final PropertyChangeListener loadingListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// propagate loading events
			if (evt.getPropertyName().equals(LoadingOverlayPane.LOADING_PROPERTY)) {
				firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
			
		}
	};
	
}
