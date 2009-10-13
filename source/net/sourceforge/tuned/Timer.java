
package net.sourceforge.tuned;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public abstract class Timer implements Runnable {
	
	private final ScheduledThreadPoolExecutor executor;
	
	private ScheduledFuture<?> scheduledFuture;
	private Thread shutdownHook;
	

	public Timer() {
		executor = new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory("Timer", Thread.NORM_PRIORITY, true));
	}
	

	public synchronized void set(long delay, TimeUnit unit, boolean runBeforeShutdown) {
		removeScheduledFuture();
		
		Runnable r = this;
		
		if (runBeforeShutdown) {
			addShutdownHook();
			
			// remove shutdown hook after execution
			r = new Runnable() {
				
				@Override
				public void run() {
					try {
						Timer.this.run();
					} finally {
						removeShutdownHook();
					}
				}
			};
		} else {
			// remove existing shutdown hook, if any
			removeShutdownHook();
		}
		
		scheduledFuture = executor.schedule(r, delay, unit);
	}
	

	public synchronized void cancel() {
		removeScheduledFuture();
		removeShutdownHook();
	}
	

	private synchronized void removeScheduledFuture() {
		if (scheduledFuture != null) {
			try {
				scheduledFuture.cancel(true);
				executor.purge();
			} finally {
				scheduledFuture = null;
			}
		}
	}
	

	private synchronized void addShutdownHook() {
		if (shutdownHook == null) {
			shutdownHook = new Thread(this);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}
	

	private synchronized void removeShutdownHook() {
		if (shutdownHook != null) {
			try {
				if (shutdownHook != Thread.currentThread()) {
					// can't remove shutdown hooks anymore, once runtime is shutting down,
					// so don't remove the shutdown hook, if we are running on the shutdown hook
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
				}
			} finally {
				shutdownHook = null;
			}
		}
	}
	
}
