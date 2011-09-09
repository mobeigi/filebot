
package net.sourceforge.filebot.cli;


import static java.lang.System.*;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class CLILogging extends Handler {
	
	public static final Logger CLILogger = createCommandlineLogger("net.sourceforge.filebot.cli");
	

	private static Logger createCommandlineLogger(String name) {
		Logger log = Logger.getLogger(name);
		log.setLevel(Level.ALL);
		
		// don't use parent handlers
		log.setUseParentHandlers(false);
		
		// CLI handler
		log.addHandler(new CLILogging());
		
		return log;
	}
	

	@Override
	public void publish(LogRecord record) {
		// print messages to stdout
		out.println(record.getMessage());
		if (record.getThrown() != null) {
			record.getThrown().printStackTrace(out);
		}
		
		// flush every message immediately
		out.flush();
	}
	

	@Override
	public void close() throws SecurityException {
		
	}
	

	@Override
	public void flush() {
		out.flush();
	}
	
}
