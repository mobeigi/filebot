
package net.sourceforge.tuned;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class PausableThreadPoolExecutor extends ThreadPoolExecutor {
	
	private boolean paused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	
	
	@SuppressWarnings("unchecked")
	public PausableThreadPoolExecutor(int nThreads, BlockingQueue<? extends Runnable> workQueue) {
		super(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, (BlockingQueue<Runnable>) workQueue);
	}
	

	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		pauseLock.lock();
		try {
			while (paused)
				unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}
	

	public void pause() {
		pauseLock.lock();
		try {
			paused = true;
		} finally {
			pauseLock.unlock();
		}
	}
	

	public void resume() {
		pauseLock.lock();
		try {
			paused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
	
	
    public boolean isPaused() {
	    return paused;
	}
}
