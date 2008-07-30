
package net.sourceforge.tuned;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;


public class MessageBus {
	
	private static final MessageBus instance = new MessageBus();
	
	
	public static MessageBus getDefault() {
		return instance;
	}
	
	private final Map<String, List<MessageHandler>> handlers = new HashMap<String, List<MessageHandler>>();
	
	
	private MessageBus() {
		
	}
	

	public synchronized void addMessageHandler(String topic, MessageHandler handler) {
		
		List<MessageHandler> list = handlers.get(topic.toLowerCase());
		
		if (list == null) {
			list = new ArrayList<MessageHandler>(3);
			handlers.put(topic.toLowerCase(), list);
		}
		
		list.add(handler);
	}
	

	public synchronized void removeMessageHandler(String topic, MessageHandler handler) {
		List<MessageHandler> list = handlers.get(topic.toLowerCase());
		
		if (list != null) {
			list.remove(handler);
		}
	}
	

	public synchronized MessageHandler[] getHandlers(String topic) {
		List<MessageHandler> list = handlers.get(topic.toLowerCase());
		
		if (list == null)
			return new MessageHandler[0];
		
		return list.toArray(new MessageHandler[0]);
	}
	

	public void publish(final String topic, final Object... messages) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				for (MessageHandler handler : getHandlers(topic.toLowerCase())) {
					handler.handle(topic.toLowerCase(), messages);
				}
			}
		});
	}
}
