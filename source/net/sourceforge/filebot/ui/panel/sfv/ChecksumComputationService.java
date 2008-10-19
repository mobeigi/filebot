
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sourceforge.tuned.DefaultThreadFactory;


public class ChecksumComputationService {
	
	public static final String ACTIVE_PROPERTY = "active";
	public static final String REMAINING_TASK_COUNT_PROPERTY = "remainingTaskCount";
	
	private final Map<File, ChecksumComputationExecutor> executors = new HashMap<File, ChecksumComputationExecutor>();
	
	private final AtomicInteger activeSessionTaskCount = new AtomicInteger(0);
	private final AtomicInteger remainingTaskCount = new AtomicInteger(0);
	
	private final ThreadFactory threadFactory;
	
	/**
	 * Property change events will be fired on the event dispatch thread
	 */
	private final SwingWorkerPropertyChangeSupport propertyChangeSupport = new SwingWorkerPropertyChangeSupport(this);
	
	
	public ChecksumComputationService() {
		this(new DefaultThreadFactory("ChecksumComputationPool", Thread.MIN_PRIORITY));
	}
	

	public ChecksumComputationService(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}
	

	public Checksum schedule(File file, File workerQueue) {
		ChecksumComputationTask task = new ChecksumComputationTask(file);
		Checksum checksum = new Checksum(task);
		
		getExecutor(workerQueue).execute(task);
		
		return checksum;
	}
	

	public void reset() {
		deactivate(true);
	}
	

	private synchronized void deactivate(boolean shutdownNow) {
		for (ChecksumComputationExecutor executor : executors.values()) {
			if (shutdownNow)
				executor.shutdownNow();
			else
				executor.shutdown();
		}
		
		executors.clear();
		
		activeSessionTaskCount.set(0);
		remainingTaskCount.set(0);
	}
	

	public boolean isActive() {
		return activeSessionTaskCount.get() > 0;
	}
	

	public int getRemainingTaskCount() {
		return remainingTaskCount.get();
	}
	

	public int getActiveSessionTaskCount() {
		return activeSessionTaskCount.get();
	}
	

	public synchronized void purge() {
		for (ChecksumComputationExecutor executor : executors.values()) {
			executor.purge();
		}
	}
	

	private synchronized ChecksumComputationExecutor getExecutor(File workerQueue) {
		ChecksumComputationExecutor executor = executors.get(workerQueue);
		
		if (executor == null) {
			executor = new ChecksumComputationExecutor();
			executors.put(workerQueue, executor);
		}
		
		return executor;
	}
	
	
	private class ChecksumComputationExecutor extends ThreadPoolExecutor {
		
		private static final int MINIMUM_POOL_SIZE = 1;
		
		
		public ChecksumComputationExecutor() {
			super(MINIMUM_POOL_SIZE, MINIMUM_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
		}
		

		private void adjustPoolSize() {
			// for a few files, use one thread
			// for lots of files, use multiple threads
			// e.g 50 files ~ 1 thread, 1000 files ~ 3 threads, 40000 files ~ 5 threads
			int preferredPoolSize = (int) Math.max(Math.log10(getQueue().size() / 10), MINIMUM_POOL_SIZE);
			
			if (getCorePoolSize() != preferredPoolSize) {
				setCorePoolSize(preferredPoolSize);
			}
		}
		

		@Override
		public void execute(Runnable command) {
			if (activeSessionTaskCount.getAndIncrement() <= 0) {
				setActive(true);
			}
			
			super.execute(command);
			
			adjustPoolSize();
			
			remainingTaskCount.incrementAndGet();
			fireRemainingTaskCountChange();
		}
		

		@Override
		public void purge() {
			try {
				List<ChecksumComputationTask> cancelledTasks = new ArrayList<ChecksumComputationTask>();
				
				for (Runnable entry : getQueue()) {
					ChecksumComputationTask task = (ChecksumComputationTask) entry;
					
					if (task.isCancelled()) {
						cancelledTasks.add(task);
					}
				}
				
				for (ChecksumComputationTask task : cancelledTasks) {
					remove(task);
				}
			} catch (ConcurrentModificationException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
		

		@Override
		public boolean remove(Runnable task) {
			boolean success = super.remove(task);
			
			if (success) {
				activeSessionTaskCount.decrementAndGet();
				
				if (remainingTaskCount.decrementAndGet() <= 0) {
					setActive(false);
				}
				
				fireRemainingTaskCountChange();
			}
			
			return success;
		}
		

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			
			if (remainingTaskCount.decrementAndGet() <= 0) {
				deactivate(false);
				setActive(false);
			}
			
			fireRemainingTaskCountChange();
		}
	}
	
	
	private void setActive(boolean active) {
		propertyChangeSupport.firePropertyChange(ACTIVE_PROPERTY, null, active);
	}
	

	private void fireRemainingTaskCountChange() {
		propertyChangeSupport.firePropertyChange(REMAINING_TASK_COUNT_PROPERTY, null, getRemainingTaskCount());
	}
	

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	
	
	private static class SwingWorkerPropertyChangeSupport extends PropertyChangeSupport {
		
		public SwingWorkerPropertyChangeSupport(Object sourceBean) {
			super(sourceBean);
		}
		

		@Override
		public void firePropertyChange(final PropertyChangeEvent evt) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					SwingWorkerPropertyChangeSupport.super.firePropertyChange(evt);
				}
				
			});
		}
	}
	
}
