
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import net.sourceforge.tuned.PausableThreadPoolExecutor;


public class ChecksumComputationExecutor {
	
	public static final String PAUSED_PROPERTY = "PAUSED_PROPERTY";
	public static final String ACTIVE_PROPERTY = "ACTIVE_PROPERTY";
	public static final String REMAINING_TASK_COUNT_PROPERTY = "REMAINING_TASK_COUNT_PROPERTY";
	public static final String ACTIVE_SESSION_TASK_COUNT_PROPERTY = "ACTIVE_SESSION_TASK_COUNT_PROPERTY";
	
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	private static final int MINIMUM_POOL_SIZE = 1;
	
	private boolean active = false;
	private long lastTaskCount = 0;
	
	private static ChecksumComputationExecutor instance = null;
	
	private LinkedBlockingQueue<ChecksumComputationTask> workQueue = new LinkedBlockingQueue<ChecksumComputationTask>();
	
	private PausableThreadPoolExecutor executor = new PausableThreadPoolExecutor(MINIMUM_POOL_SIZE, workQueue) {
		
		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			fireRemainingTaskCountChange();
		}
		

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			// afterExecute is called from the same thread, the runnable is called,
			// so there is at least one active thread
			if (workQueue.isEmpty() && getActiveCount() <= 1)
				setActive(false);
		}
	};
	
	
	private ChecksumComputationExecutor() {
		
	}
	

	public static synchronized ChecksumComputationExecutor getInstance() {
		if (instance == null)
			instance = new ChecksumComputationExecutor();
		
		return instance;
	}
	

	public void execute(ChecksumComputationTask task) {
		setActive(true);
		
		adjustPoolSize();
		
		executor.execute(task);
		fireActiveSessionTaskCountChange();
	}
	

	/**
	 * increase/decrease the core pool size depending on number of queued tasks
	 */
	private void adjustPoolSize() {
		// for only a few files, use only one thread
		// for lots of files, use multiple threads
		// e.g 200 files ~ 1 thread, 1000 files ~ 2 threads, 40000 files ~ 4 threads
		int recommendedPoolSize = (int) Math.max(Math.log10(Math.max(workQueue.size(), 1)), MINIMUM_POOL_SIZE);
		
		if (executor.getCorePoolSize() != recommendedPoolSize)
			executor.setCorePoolSize(recommendedPoolSize);
	}
	

	public void resume() {
		if (!isPaused())
			return;
		
		executor.resume();
		
		// invoke property change events on EDT
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(PAUSED_PROPERTY, null, executor.isPaused()));
	}
	

	public void pause() {
		if (isPaused())
			return;
		
		executor.pause();
		
		// invoke property change events on EDT
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(PAUSED_PROPERTY, null, executor.isPaused()));
	}
	

	private synchronized void setActive(boolean b) {
		if (this.active == b)
			return;
		
		this.active = b;
		
		if (!this.active) {
			// end of active computing session
			lastTaskCount = executor.getTaskCount();
			
			// reset pool size
			adjustPoolSize();
		}
		
		// invoke property change events on EDT
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(ACTIVE_PROPERTY, null, active));
	}
	

	private void fireRemainingTaskCountChange() {
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(REMAINING_TASK_COUNT_PROPERTY, null, getRemainingTaskCount()));
	}
	

	private void fireActiveSessionTaskCountChange() {
		SwingUtilities.invokeLater(new FirePropertyChangeRunnable(ACTIVE_SESSION_TASK_COUNT_PROPERTY, null, getActiveSessionTaskCount()));
	}
	

	public boolean isPaused() {
		return executor.isPaused();
	}
	

	public boolean isActive() {
		return active;
	}
	

	/**
	 * 
	 * @return Number of remaining tasks
	 */
	public int getRemainingTaskCount() {
		return workQueue.size() + executor.getActiveCount();
	}
	

	public int getActiveSessionTaskCount() {
		return (int) (executor.getTaskCount() - lastTaskCount);
	}
	

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	
	
	private class FirePropertyChangeRunnable implements Runnable {
		
		private String property;
		private Object oldValue;
		private Object newValue;
		
		
		public FirePropertyChangeRunnable(String property, Object oldValue, Object newValue) {
			this.property = property;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
		

		@Override
		public void run() {
			propertyChangeSupport.firePropertyChange(property, oldValue, newValue);
		}
	}
	
}
