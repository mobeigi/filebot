
package net.sourceforge.filebot.ui.panel.sfv;


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

import javax.swing.SwingUtilities;

import net.sourceforge.tuned.DefaultThreadFactory;


public class ChecksumComputationService {
	
	public static final String ACTIVE_PROPERTY = "ACTIVE_PROPERTY";
	public static final String REMAINING_TASK_COUNT_PROPERTY = "REMAINING_TASK_COUNT_PROPERTY";
	
	private static final ThreadFactory checksumComputationThreadFactory = new DefaultThreadFactory("ChecksumComputationPool", Thread.MIN_PRIORITY);
	
	private static final ChecksumComputationService service = new ChecksumComputationService();
	
	
	public static ChecksumComputationService getService() {
		return service;
	}
	
	private final Map<File, ChecksumComputationExecutor> executors = new HashMap<File, ChecksumComputationExecutor>();
	
	private final AtomicInteger activeSessionTaskCount = new AtomicInteger(0);
	private final AtomicInteger remainingTaskCount = new AtomicInteger(0);
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	
	private ChecksumComputationService() {
		
	}
	

	public Checksum getChecksum(File file, File workerQueueKey) {
		ChecksumComputationTask task = new ChecksumComputationTask(file);
		Checksum checksum = new Checksum(task);
		
		getExecutor(workerQueueKey).execute(task);
		
		return checksum;
	}
	

	public void reset() {
		deactivate(true);
	}
	

	private void deactivate(boolean shutdownNow) {
		synchronized (executors) {
			for (ChecksumComputationExecutor executor : executors.values()) {
				if (shutdownNow) {
					executor.shutdownNow();
				} else {
					executor.shutdown();
				}
			}
			
			executors.clear();
			
			activeSessionTaskCount.set(0);
			remainingTaskCount.set(0);
		}
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
	

	public void purge() {
		synchronized (executors) {
			for (ChecksumComputationExecutor executor : executors.values()) {
				executor.purge();
			}
		}
	}
	

	private ChecksumComputationExecutor getExecutor(File workerQueueKey) {
		synchronized (executors) {
			ChecksumComputationExecutor executor = executors.get(workerQueueKey);
			
			if (executor == null) {
				executor = new ChecksumComputationExecutor();
				executors.put(workerQueueKey, executor);
			}
			
			return executor;
		}
	}
	
	
	private class ChecksumComputationExecutor extends ThreadPoolExecutor {
		
		private static final int MINIMUM_POOL_SIZE = 1;
		
		
		public ChecksumComputationExecutor() {
			super(MINIMUM_POOL_SIZE, MINIMUM_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), checksumComputationThreadFactory);
		}
		

		private void adjustPoolSize() {
			// for only a few files, use one thread
			// for lots of files, use multiple threads
			// e.g 50 files ~ 1 thread, 1000 files ~ 3 threads, 40000 files ~ 5 threads
			
			int preferredPoolSize = MINIMUM_POOL_SIZE;
			
			int queueSize = getQueue().size();
			
			if (queueSize > 0) {
				preferredPoolSize += Math.log10(Math.max(queueSize / 10, 1));
			}
			
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
			} catch (ConcurrentModificationException ex) {
				
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
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(ACTIVE_PROPERTY, active));
	}
	

	private void fireRemainingTaskCountChange() {
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(REMAINING_TASK_COUNT_PROPERTY, getRemainingTaskCount()));
	}
	

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	
	
	private class FirePropertyChangeRunnable implements Runnable {
		
		private final String property;
		private final Object newValue;
		
		
		public FirePropertyChangeRunnable(String property, Object newValue) {
			this.property = property;
			this.newValue = newValue;
		}
		

		@Override
		public void run() {
			propertyChangeSupport.firePropertyChange(property, null, newValue);
		}
	}
	
}
