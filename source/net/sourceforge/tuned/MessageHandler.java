
package net.sourceforge.tuned;




public interface MessageHandler {
	
	public void handle(String topic, String... messages);
	
}
