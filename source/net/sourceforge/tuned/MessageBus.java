
package net.sourceforge.tuned;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;


public class MessageBus {
	
	private static final MessageBus instance = new MessageBus();
	
	
	public static MessageBus getDefault() {
		return instance;
	}
	
	private final Map<String, List<MessageHandler>> handlers = new HashMap<String, List<MessageHandler>>();
	
	
	public synchronized void addMessageHandler(String topic, MessageHandler handler) {
		if (handler == null)
			return;
		
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
		if (SwingUtilities.isEventDispatchThread()) {
			publishDirect(topic, messages);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					publishDirect(topic, messages);
				}
			});
		}
	}
	

	private void publishDirect(String topic, Object... messages) {
		for (MessageHandler handler : getHandlers(topic.toLowerCase())) {
			try {
				handler.handle(topic.toLowerCase(), messages);
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
}
