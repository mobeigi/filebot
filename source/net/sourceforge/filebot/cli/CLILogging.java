package net.sourceforge.filebot.cli;

import static java.lang.System.*;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.codehaus.groovy.runtime.StackTraceUtils;

class CLILogging extends Handler {

	public static final Logger CLILogger = createCommandlineLogger("net.sourceforge.filebot.logger.cli");

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
		if (record.getLevel().intValue() <= getLevel().intValue())
			return;

		// make sure all previous messages are already flushed
		System.out.flush();
		System.err.flush();

		// use either System.out or System.err depending on the severity of the error
		PrintStream out = record.getLevel().intValue() < Level.WARNING.intValue() ? System.out : System.err;

		// print messages
		out.println(record.getMessage());
		if (record.getThrown() != null) {
			StackTraceUtils.deepSanitize(record.getThrown()).printStackTrace(out);
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
