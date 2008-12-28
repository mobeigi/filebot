
package net.sourceforge.filebot.ui;


import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.notification.MessageNotification;
import net.sourceforge.tuned.ui.notification.NotificationManager;
import net.sourceforge.tuned.ui.notification.QueueNotificationLayout;


public class NotificationLoggingHandler extends Handler {
	
	public final NotificationManager notificationManager;
	public final int timeout = 2500;
	
	
	public NotificationLoggingHandler() {
		this(new NotificationManager(new QueueNotificationLayout(SwingConstants.NORTH, SwingConstants.SOUTH)));
	}
	

	public NotificationLoggingHandler(NotificationManager notificationManager) {
		this.notificationManager = notificationManager;
	}
	

	@Override
	public void publish(final LogRecord record) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				Level level = record.getLevel();
				
				if (level == Level.INFO) {
					show(record.getMessage(), ResourceManager.getIcon("message.info"), timeout * 1);
				} else if (level == Level.WARNING) {
					show(record.getMessage(), ResourceManager.getIcon("message.warning"), timeout * 2);
				} else if (level == Level.SEVERE) {
					show(record.getMessage(), ResourceManager.getIcon("message.error"), timeout * 3);
				}
			}
		});
	}
	

	private void show(String message, Icon icon, int timeout) {
		notificationManager.show(new MessageNotification(FileBotUtil.getApplicationName(), message, icon, timeout));
	}
	

	@Override
	public void close() throws SecurityException {
		
	}
	

	@Override
	public void flush() {
		
	}
	
}
