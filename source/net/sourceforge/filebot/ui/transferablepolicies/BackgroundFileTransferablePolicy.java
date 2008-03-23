
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;


public abstract class BackgroundFileTransferablePolicy<V> extends FileTransferablePolicy {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private SingleThreadExecutor executor = null;
	
	
	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files == null)
			return;
		
		if (!add)
			clear();
		
		submit(new LoadFilesTask(files));
	}
	

	protected void submit(Runnable task) {
		synchronized (this) {
			if (executor == null) {
				executor = new SingleThreadExecutor();
			}
		}
		
		executor.submit(task);
	}
	

	public boolean isActive() {
		synchronized (this) {
			if (executor == null)
				return false;
			
			return executor.isActive();
		}
	}
	

	public void cancelAll() {
		synchronized (this) {
			if (executor != null) {
				// interrupt all threads
				executor.shutdownNow();
				executor = null;
			}
		}
	}
	

	/**
	 * Sends data chunks to the process method.
	 * 
	 * @param chunks
	 */
	protected final void publish(V... chunks) {
		SwingUtilities.invokeLater(new ProcessChunksTask(chunks, Thread.currentThread()));
	}
	

	/**
	 * Receives data chunks from the publish method asynchronously on the Event Dispatch
	 * Thread.
	 * 
	 * @param chunks
	 */
	protected abstract void process(List<V> chunks);
	
	
	private class LoadFilesTask implements Runnable {
		
		private List<File> files;
		
		
		public LoadFilesTask(List<File> files) {
			this.files = files;
		}
		

		@Override
		public void run() {
			load(files);
		}
	}
	

	private class ProcessChunksTask implements Runnable {
		
		private V[] chunks;
		private Thread publisher;
		
		
		public ProcessChunksTask(V[] chunks, Thread publisher) {
			this.chunks = chunks;
			this.publisher = publisher;
		}
		

		@Override
		public void run() {
			if (!publisher.isInterrupted() && publisher.isAlive()) {
				process(Arrays.asList(chunks));
			}
		}
	}
	

	private class SingleThreadExecutor extends ThreadPoolExecutor {
		
		private final AtomicInteger count = new AtomicInteger(0);
		
		
		public SingleThreadExecutor() {
			super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		}
		

		public boolean isActive() {
			return count.get() > 0;
		}
		

		@Override
		public void execute(Runnable command) {
			
			if (count.getAndIncrement() <= 0) {
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						propertyChangeSupport.firePropertyChange(LOADING_PROPERTY, false, true);
					}
				});
			}
			
			super.execute(command);
		}
		

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			
			if (count.decrementAndGet() <= 0) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						propertyChangeSupport.firePropertyChange(LOADING_PROPERTY, true, false);
					}
				});
			}
		}
		
	}
	
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}
	
}
