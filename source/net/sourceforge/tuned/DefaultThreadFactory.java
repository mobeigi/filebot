
package net.sourceforge.tuned;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class DefaultThreadFactory implements ThreadFactory {
	
	private final AtomicInteger threadNumber = new AtomicInteger(0);
	private final ThreadGroup group;
	
	private final int priority;
	private final boolean daemon;
	
	
	public DefaultThreadFactory(String name, int priority) {
		this(name, priority, false);
	}
	

	public DefaultThreadFactory(String groupName, int priority, boolean daemon) {
		group = new ThreadGroup(groupName);
		group.setDaemon(daemon);
		
		this.daemon = daemon;
		this.priority = priority;
	}
	

	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, String.format("%s-thread-%d", group.getName(), threadNumber.incrementAndGet()));
		
		t.setDaemon(daemon);
		t.setPriority(priority);
		
		return t;
	}
	
}
