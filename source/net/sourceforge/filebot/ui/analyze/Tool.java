
package net.sourceforge.filebot.ui.analyze;


import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.ui.analyze.FileTree.FileNode;
import net.sourceforge.filebot.ui.analyze.FileTree.FolderNode;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.LoadingOverlayPane;


abstract class Tool<M> extends JComponent {
	
	private UpdateModelTask updateTask = null;
	
	
	public Tool(String name) {
		setName(name);
	}
	
	
	public void setSourceModel(FolderNode sourceModel) {
		if (updateTask != null) {
			updateTask.cancel(true);
		}
		
		Tool.this.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, false, true);
		updateTask = new UpdateModelTask(sourceModel);
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
			return createModelInBackground(sourceModel);
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
					} else {
						// should not happen
						Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
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
