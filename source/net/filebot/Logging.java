package net.filebot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.StackTraceUtils;

import net.filebot.util.SystemProperty;

public final class Logging {

	private static final SystemProperty<Pattern> anonymizePattern = SystemProperty.of("net.filebot.logging.anonymize", Pattern::compile);
	private static final SystemProperty<Level> debugLevel = SystemProperty.of("net.filebot.logging.debug", Level::parse, Level.WARNING);

	public static final Logger log = createConsoleLogger("net.filebot.console", Level.ALL);
	public static final Logger debug = createConsoleLogger("net.filebot.debug", debugLevel.get());

	public static Logger createConsoleLogger(String name, Level level) {
		Logger log = Logger.getLogger(name);
		log.setUseParentHandlers(false);
		log.setLevel(level);
		log.addHandler(createConsoleHandler(level));
		return log;
	}

	public static StreamHandler createSimpleFileHandler(File file, Level level) throws IOException {
		StreamHandler handler = new StreamHandler(new FileOutputStream(file, true), new SimpleFormatter());
		handler.setLevel(level);
		return handler;
	}

	public static ConsoleHandler createConsoleHandler(Level level) {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(level);
		handler.setFormatter(new ConsoleFormatter(anonymizePattern.get()));
		return handler;
	}

	public static Supplier<String> format(String format, Object... args) {
		return () -> String.format(format, args);
	}

	public static Supplier<String> trace(Throwable t) {
		return () -> {
			StringBuilder s = new StringBuilder();
			s.append(t.getClass().getSimpleName()).append(": ");
			s.append(t.getMessage());

			StackTraceElement[] trace = StackTraceUtils.sanitizeRootCause(t).getStackTrace();
			if (trace != null && trace.length > 0) {
				s.append(" at ").append(trace[0]);
			}
			return s.toString();
		};
	}

	public static class ConsoleHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			// use either System.out or System.err depending on the severity of the error
			PrintStream out = record.getLevel().intValue() < Level.WARNING.intValue() ? System.out : System.err;

			// print messages
			out.print(getFormatter().format(record));

			Throwable thrown = record.getThrown();
			if (thrown != null) {
				StackTraceUtils.deepSanitize(thrown).printStackTrace(out);
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
			if (anonymize != null && message != null) {
				message = anonymize.matcher(message).replaceAll("");
			}
			return message + System.lineSeparator();
		}
	}

}
