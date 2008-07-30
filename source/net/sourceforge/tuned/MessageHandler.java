
package net.sourceforge.tuned;


public interface MessageHandler {
	
	public void handle(String topic, Object... messages);
	
}
