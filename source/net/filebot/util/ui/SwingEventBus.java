package net.filebot.util.ui;

import static net.filebot.Logging.*;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionContext;

public class SwingEventBus extends AsyncEventBus {

	private static SwingEventBus instance;

	public static synchronized SwingEventBus getInstance() {
		if (instance == null) {
			instance = new SwingEventBus();
		}
		return instance;
	}

	public SwingEventBus() {
		super(SwingUtilities::invokeLater, SwingEventBus::handleException);
	}

	protected static void handleException(Throwable throwable, SubscriberExceptionContext context) {
		debug.log(Level.WARNING, "Failed to handle event: " + context.getEvent(), throwable);
	}

}
