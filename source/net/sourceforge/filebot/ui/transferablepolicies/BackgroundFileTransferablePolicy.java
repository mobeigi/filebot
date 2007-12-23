
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.List;

import javax.swing.SwingWorker;

import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;


public abstract class BackgroundFileTransferablePolicy<V> extends FileTransferablePolicy {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private BackgroundWorker backgroundWorker;
	
	
	@Override
	public boolean handleTransferable(Transferable tr, boolean add) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files == null)
			return false;
		
		if (!add)
			clear();
		
		backgroundWorker = new BackgroundWorker(files);
		backgroundWorker.addPropertyChangeListener(new BackgroundWorkerListener());
		backgroundWorker.execute();
		
		return true;
	}
	

	/**
	 * Sends data chunks to the process method.
	 * 
	 * @param chunks
	 */
	protected final void publish(V... chunks) {
		backgroundWorker.publishChunks(chunks);
	}
	

	/**
	 * Receives data chunks from the publish method asynchronously on the Event Dispatch Thread.
	 * 
	 * @param chunks
	 */
	protected void process(List<V> chunks) {
		
	}
	
	
	private class BackgroundWorker extends SwingWorker<Object, V> {
		
		private List<File> files;
		
		
		public BackgroundWorker(List<File> files) {
			this.files = files;
		}
		

		@Override
		protected void process(List<V> chunks) {
			BackgroundFileTransferablePolicy.this.process(chunks);
		}
		

		/**
		 * make publish() accessible
		 * 
		 * @param chunks
		 */
		public void publishChunks(V... chunks) {
			super.publish(chunks);
		}
		

		@Override
		protected Object doInBackground() throws Exception {
			load(files);
			return null;
		}
	}
	

	private class BackgroundWorkerListener extends SwingWorkerPropertyChangeAdapter {
		
		@Override
		public void started(PropertyChangeEvent evt) {
			setEnabled(false);
			firePropertyChange(LOADING_PROPERTY, null, true);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			firePropertyChange(LOADING_PROPERTY, null, false);
			setEnabled(true);
		}
	}
	
}
