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

	private static final SystemProperty<Level> debugLevel = SystemProperty.of("net.filebot.logging.debug", Level::parse, Level.WARNING);
	private static final SystemProperty<Pattern> anonymizePattern = SystemProperty.of("net.filebot.logging.anonymize", Pattern::compile);
	private static final SystemProperty<Boolean> color = SystemProperty.of("net.filebot.logging.color", Boolean::parseBoolean, Color.isSupported());

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
		ConsoleHandler handler = color.get() ? new AnsiColorConsoleHandler() : new ConsoleHandler();
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

	public static class ConsoleHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			// use either System.out or System.err depending on the severity of the error
			PrintStream out = record.getLevel().intValue() < Level.WARNING.intValue() ? System.out : System.err;

			// print messages to selected output stream
			print(record, out);

			// flush every message immediately
			out.flush();
		}

		public void print(LogRecord record, PrintStream out) {
			out.print(getFormatter().format(record));

			Throwable thrown = record.getThrown();
			if (thrown != null) {
				StackTraceUtils.deepSanitize(thrown).printStackTrace(out);
			}
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

	public static class AnsiColorConsoleHandler extends ConsoleHandler {

		@Override
		public void print(LogRecord record, PrintStream out) {
			Color c = getColor(record.getLevel().intValue());

			if (c == null) {
				super.print(record, out);
			} else {
				out.print(c.head);
				super.print(record, out);
				out.print(c.tail);
			}
		}

		public Color getColor(int level) {
			if (level < Level.FINE.intValue())
				return Color.LIME_GREEN;
			if (level < Level.INFO.intValue())
				return Color.ROYAL_BLUE;
			if (level < Level.WARNING.intValue())
				return null;
			if (level < Level.SEVERE.intValue())
				return Color.ORANGE_RED;

			return Color.CHERRY_RED; // SEVERE
		}

	}

	public static class Color {

		public static final Color CHERRY_RED = new Color(0xC4);
		public static final Color ORANGE_RED = new Color(0xCA);
		public static final Color ROYAL_BLUE = new Color(0x28);
		public static final Color LIME_GREEN = new Color(0x27);

		public final String head;
		public final String tail;

		Color(int color) {
			this("38;5;" + color); // see https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
		}

		Color(String code) {
			this.head = "\u001b[" + code + "m";
			this.tail = "\u001b[0m";
		}

		public String colorize(String message) {
			return head + message + tail;
		}

		public static boolean isSupported() {
			// enable ANSI colors on a standard terminal (if running with interactive console)
			return System.console() != null && "xterm-256color".equals(System.getenv("TERM"));
		}

	}

}
