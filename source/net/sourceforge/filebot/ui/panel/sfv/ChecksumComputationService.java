
package net.sourceforge.filebot.ui.panel.sfv;


import static java.lang.Math.log10;
import static java.lang.Math.max;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.tuned.DefaultThreadFactory;


class ChecksumComputationService {
	
	public static final String TASK_COUNT_PROPERTY = "taskCount";
	
	private final List<ThreadPoolExecutor> executors = new ArrayList<ThreadPoolExecutor>();
	
	private final AtomicInteger completedTaskCount = new AtomicInteger(0);
	private final AtomicInteger totalTaskCount = new AtomicInteger(0);
	
	
	public ExecutorService newExecutor() {
		return new ChecksumComputationExecutor();
	}
	

	public void reset() {
		synchronized (executors) {
			for (ExecutorService executor : executors) {
				executor.shutdownNow();
			}
			
			totalTaskCount.set(0);
			completedTaskCount.set(0);
			
			executors.clear();
		}
		
		fireTaskCountChanged();
	}
	

	public int getTaskCount() {
		return getTotalTaskCount() - getCompletedTaskCount();
	}
	

	public int getTotalTaskCount() {
		return totalTaskCount.get();
	}
	

	public int getCompletedTaskCount() {
		return completedTaskCount.get();
	}
	

	public void purge() {
		synchronized (executors) {
			for (ThreadPoolExecutor executor : executors) {
				executor.purge();
			}
		}
	}
	
	
	private class ChecksumComputationExecutor extends ThreadPoolExecutor {
		
		public ChecksumComputationExecutor() {
			super(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("ChecksumComputationPool", Thread.MIN_PRIORITY));
			
			synchronized (executors) {
				executors.add(this);
			}
			
			prestartAllCoreThreads();
		}
		

		protected int getPreferredPoolSize() {
			// for a few files, use one thread
			// for lots of files, use multiple threads
			// e.g 50 files ~ 1 thread, 200 files ~ 2 threads, 1000 files ~ 3 threads, 40000 files ~ 5 threads
			return max((int) log10(getQueue().size()), 1);
		}
		

		@Override
		public void execute(Runnable command) {
			int preferredPoolSize = getPreferredPoolSize();
			
			if (getCorePoolSize() < preferredPoolSize) {
				setCorePoolSize(preferredPoolSize);
			}
			
			synchronized (this) {
				super.execute(command);
			}
			
			totalTaskCount.incrementAndGet();
			fireTaskCountChanged();
		}
		

		@Override
		public void purge() {
			int delta = 0;
			
			synchronized (this) {
				delta += getQueue().size();
				super.purge();
				delta -= getQueue().size();
			}
			
			if (delta > 0) {
				// subtract removed tasks from task count
				totalTaskCount.addAndGet(-delta);
				fireTaskCountChanged();
			}
		}
		

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			
			completedTaskCount.incrementAndGet();
			fireTaskCountChanged();
		}
		

		@Override
		protected void terminated() {
			synchronized (executors) {
				executors.remove(this);
			}
		}
		
	}
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	
	private void fireTaskCountChanged() {
		propertyChangeSupport.firePropertyChange(TASK_COUNT_PROPERTY, null, getTaskCount());
	}
	

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}
	
}
