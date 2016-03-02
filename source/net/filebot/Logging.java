package net.filebot;

import java.io.PrintStream;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.filebot.util.SystemProperty;

import org.codehaus.groovy.runtime.StackTraceUtils;

public final class Logging {

	private static final SystemProperty<Pattern> anonymizePattern = SystemProperty.of("net.filebot.logging.anonymize", Pattern::compile);
	private static final SystemProperty<Level> debugLevel = SystemProperty.of("net.filebot.logging.debug", Level::parse, Level.CONFIG);

	public static final Logger log = getConsoleLogger("net.filebot.console", Level.ALL, new ConsoleFormatter(anonymizePattern.get()));
	public static final Logger debug = getConsoleLogger("net.filebot.debug", debugLevel.get(), new ConsoleFormatter(anonymizePattern.get()));

	public static Logger getConsoleLogger(String name, Level level, Formatter formatter) {
		Logger log = Logger.getLogger(name);
		log.setLevel(level);
		log.setUseParentHandlers(false);
		log.addHandler(new ConsoleHandler(level, formatter));
		return log;
	}

	public static Supplier<String> format(String format, Object... args) {
		return () -> String.format(format, args);
	}

	public static class ConsoleHandler extends Handler {

		public ConsoleHandler(Level level, Formatter formatter) {
			setLevel(level);
			setFormatter(formatter);
		}

		@Override
		public void publish(LogRecord record) {
			// use either System.out or System.err depending on the severity of the error
			PrintStream out = record.getLevel().intValue() < Level.WARNING.intValue() ? System.out : System.err;

			// print messages
			out.print(getFormatter().format(record));

			Throwable t = record.getThrown();
			if (t != null) {
				StackTraceUtils.deepSanitize(t).printStackTrace(out);
			}

			// flush every message immediately
			out.flush();
		}

		@Override
		public void flush() {
			System.out.flush();
			System.err.flush();
		}

		@Override
		public void close() throws SecurityException {

		}

	}

	public static class ConsoleFormatter extends Formatter {

		private final Pattern anonymize;

		public ConsoleFormatter(Pattern anonymize) {
			this.anonymize = anonymize;
		}

		@Override
		public String format(LogRecord record) {
			String message = record.getMessage();
			if (anonymize != null) {
				message = anonymize.matcher(message).replaceAll("");
			}
			return message + System.lineSeparator();
		}
	}

}
