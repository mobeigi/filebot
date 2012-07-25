
package net.sourceforge.filebot.ui;


import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.tuned.ui.notification.Direction.*;

import java.awt.GraphicsEnvironment;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.notification.MessageNotification;
import net.sourceforge.tuned.ui.notification.NotificationManager;
import net.sourceforge.tuned.ui.notification.QueueNotificationLayout;


public class NotificationLogging extends Handler {
	
	public static final Logger UILogger = createNotificationLogger("net.sourceforge.filebot.logger.ui");
	
	
	private static Logger createNotificationLogger(String name) {
		Logger log = Logger.getLogger(name);
		
		// don't use parent handlers
		log.setUseParentHandlers(false);
		
		// ui handler
		log.addHandler(new NotificationLogging());
		
		// console handler (for warnings and errors only)
		ConsoleHandler console = new ConsoleHandler();
		console.setLevel(Level.WARNING);
		log.addHandler(console);
		
		return log;
	}
	
	public final NotificationManager notificationManager;
	public final int timeout = 2500;
	
	
	public NotificationLogging() {
		this(new NotificationManager(new QueueNotificationLayout(NORTH, SOUTH)));
	}
	
	
	public NotificationLogging(NotificationManager notificationManager) {
		this.notificationManager = notificationManager;
	}
	
	
	@Override
	public void publish(LogRecord record) {
		// fail gracefully on an headless machine
		if (GraphicsEnvironment.isHeadless())
			return;
		
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
		
		if ((message == null || message.isEmpty()) && record.getThrown() != null) {
			// if message is empty, display exception string
			return ExceptionUtilities.getMessage(record.getThrown());
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
