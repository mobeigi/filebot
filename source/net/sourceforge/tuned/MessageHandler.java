
package net.sourceforge.tuned;


import java.util.EventListener;


public interface MessageHandler extends EventListener {
	
	public void handle(String topic, String... messages);
	
}
