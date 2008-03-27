
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import net.sourceforge.tuned.DefaultThreadFactory;


public abstract class BackgroundFileTransferablePolicy<V> extends FileTransferablePolicy {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private static final ThreadFactory backgroundTransferThreadFactory = new DefaultThreadFactory("BackgroundTransferPool", Thread.NORM_PRIORITY);
	
	private SingleThreadExecutor executor = null;
	
	private final AtomicInteger count = new AtomicInteger(0);
	
	
	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files == null)
			return;
		
		if (!add)
			clear();
		
		execute(new LoadFilesTask(files));
	}
	

	protected synchronized void execute(Runnable task) {
		if (executor == null) {
			executor = new SingleThreadExecutor();
		}
		
		executor.execute(task);
	}
	

	public boolean isActive() {
		return count.get() > 0;
	}
	

	private synchronized void setActive(final boolean active) {
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				propertyChangeSupport.firePropertyChange(LOADING_PROPERTY, null, active);
			}
		});
	}
	

	private synchronized void deactivate(boolean shutdownNow) {
		if (executor != null) {
			if (shutdownNow) {
				executor.shutdownNow();
			} else {
				executor.shutdown();
			}
			
			executor = null;
		}
		
		count.set(0);
	}
	

	public synchronized void reset() {
		deactivate(true);
		setActive(false);
	}
	

	/**
	 * Sends data chunks to the process method.
	 * 
	 * @param chunks
	 */
	protected final void publish(V... chunks) {
		SwingUtilities.invokeLater(new ProcessChunksTask(chunks));
	}
	

	/**
	 * Receives data chunks from the publish method asynchronously on the Event Dispatch
	 * Thread.
	 * 
	 * @param chunks
	 */
	protected abstract void process(List<V> chunks);
	
	
	private class LoadFilesTask implements Runnable {
		
		private final List<File> files;
		
		
		public LoadFilesTask(List<File> files) {
			this.files = files;
		}
		

		@Override
		public void run() {
			load(files);
		}
	}
	

	private class ProcessChunksTask implements Runnable {
		
		private final V[] chunks;
		
		
		public ProcessChunksTask(V[] chunks) {
			this.chunks = chunks;
		}
		

		@Override
		public void run() {
			process(Arrays.asList(chunks));
		}
	}
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}
	
	
	private class SingleThreadExecutor extends ThreadPoolExecutor {
		
		public SingleThreadExecutor() {
			super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), backgroundTransferThreadFactory);
		}
		

		@Override
		public void execute(Runnable command) {
			
			if (count.getAndIncrement() <= 0) {
				setActive(true);
			}
			
			super.execute(command);
		}
		

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			
			if (count.decrementAndGet() <= 0) {
				// shutdown executor
				deactivate(false);
				setActive(false);
			}
		}
		
	}
	
}
