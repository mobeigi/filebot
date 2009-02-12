
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;


public abstract class BackgroundFileTransferablePolicy<V> extends FileTransferablePolicy {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private final ThreadLocal<BackgroundWorker> threadLocalWorker = new ThreadLocal<BackgroundWorker>() {
		
		@Override
		protected BackgroundWorker initialValue() {
			// fail if a non-background-worker thread is trying to access the thread-local worker object
			throw new IllegalThreadStateException("Illegal access thread");
		}
	};
	
	private final List<BackgroundWorker> workers = new ArrayList<BackgroundWorker>(2);
	
	
	@Override
	public void handleTransferable(Transferable tr, TransferAction action) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (action != TransferAction.ADD)
			clear();
		
		// create and start worker
		new BackgroundWorker(files).execute();
	}
	

	@Override
	protected void clear() {
		// stop other workers on clear (before starting new worker)
		reset();
	}
	

	public void reset() {
		synchronized (workers) {
			if (workers.size() > 0) {
				// avoid ConcurrentModificationException by iterating over a copy
				for (BackgroundWorker worker : new ArrayList<BackgroundWorker>(workers)) {
					// worker.cancel() will invoke worker.done() which will invoke workers.remove(worker)
					worker.cancel(true);
				}
			}
		}
	}
	

	protected abstract void process(List<V> chunks);
	

	protected abstract void process(Exception e);
	

	protected final void publish(V... chunks) {
		threadLocalWorker.get().offer(chunks);
	}
	
	
	protected class BackgroundWorker extends SwingWorker<Object, V> {
		
		private final List<File> files;
		
		
		public BackgroundWorker(List<File> files) {
			this.files = files;
			
			// register this worker
			synchronized (workers) {
				if (workers.add(this) && workers.size() == 1) {
					swingPropertyChangeSupport.firePropertyChange(LOADING_PROPERTY, false, true);
				}
			}
		}
		

		@Override
		protected Object doInBackground() {
			// associate this worker with the current (background) thread
			threadLocalWorker.set(this);
			
			try {
				load(files);
			} finally {
				threadLocalWorker.remove();
			}
			
			return null;
		}
		

		public void offer(V... chunks) {
			if (!isCancelled()) {
				publish(chunks);
			}
		}
		

		@Override
		protected void process(List<V> chunks) {
			if (!isCancelled()) {
				BackgroundFileTransferablePolicy.this.process(chunks);
			}
		}
		

		@Override
		protected void done() {
			// unregister worker
			synchronized (workers) {
				if (workers.remove(this) && workers.isEmpty()) {
					swingPropertyChangeSupport.firePropertyChange(LOADING_PROPERTY, true, false);
				}
			}
			
			if (!isCancelled()) {
				try {
					// check for exception
					get();
				} catch (Exception e) {
					BackgroundFileTransferablePolicy.this.process(e);
				}
			}
		}
	}
	
	protected final PropertyChangeSupport swingPropertyChangeSupport = new PropertyChangeSupport(this) {
		
		@Override
		public void firePropertyChange(final PropertyChangeEvent evt) {
			if (SwingUtilities.isEventDispatchThread()) {
				super.firePropertyChange(evt);
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						swingPropertyChangeSupport.firePropertyChange(evt);
					}
				});
			}
		}
		
	};
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		swingPropertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		swingPropertyChangeSupport.removePropertyChangeListener(listener);
	}
}
