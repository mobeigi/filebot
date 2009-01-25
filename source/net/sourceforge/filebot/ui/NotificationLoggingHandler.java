
package net.sourceforge.filebot.ui;


import static net.sourceforge.filebot.Settings.getApplicationName;
import static net.sourceforge.tuned.ui.notification.Direction.NORTH;
import static net.sourceforge.tuned.ui.notification.Direction.SOUTH;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.notification.MessageNotification;
import net.sourceforge.tuned.ui.notification.NotificationManager;
import net.sourceforge.tuned.ui.notification.QueueNotificationLayout;


public class NotificationLoggingHandler extends Handler {
	
	public final NotificationManager notificationManager;
	public final int timeout = 2500;
	
	
	public NotificationLoggingHandler() {
		this(new NotificationManager(new QueueNotificationLayout(NORTH, SOUTH)));
	}
	

	public NotificationLoggingHandler(NotificationManager notificationManager) {
		this.notificationManager = notificationManager;
	}
	

	@Override
	public void publish(LogRecord record) {
		final Level level = record.getLevel();
		final String message = getMessage(record);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				if (level == Level.INFO) {
					show(message, ResourceManager.getIcon("message.info"), timeout * 1);
				} else if (level == Level.WARNING) {
					show(message, ResourceManager.getIcon("message.warning"), timeout * 2);
				} else if (level == Level.SEVERE) {
					show(message, ResourceManager.getIcon("message.error"), timeout * 3);
				}
			}
		});
	}
	

	protected String getMessage(LogRecord record) {
		String message = record.getMessage();
		
		if (message == null || message.isEmpty()) {
			// if message is empty, display exception string
			message = record.getThrown().toString();
		}
		
		return message;
	}
	

	protected void show(String message, Icon icon, int timeout) {
		notificationManager.show(new MessageNotification(getApplicationName(), message, icon, timeout));
	}
	

	@Override
	public void close() throws SecurityException {
		
	}
	

	@Override
	public void flush() {
		
	}
	
}
