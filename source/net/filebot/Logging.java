package net.filebot;

import static java.nio.channels.Channels.*;
import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
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
		handler.setEncoding("UTF-8");
		handler.setLevel(level);
		return handler;
	}

	public static StreamHandler createLogFileHandler(File file, boolean lock, Level level) throws IOException {
		if (!file.exists() && !file.getParentFile().mkdirs() && !file.createNewFile()) {
			throw new IOException("Failed to create log file: " + file);
		}

		// open file channel and lock
		FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		if (lock) {
			try {
				log.config("Locking " + file);
				channel.lock();
			} catch (Exception e) {
				throw new IOException("Failed to acquire lock: " + file, e);
			}
		}

		StreamHandler handler = new StreamHandler(newOutputStream(channel), new ConsoleFormatter(anonymizePattern.get(), false));
		handler.setEncoding("UTF-8");
		handler.setLevel(level);
		return handler;
	}

	public static ConsoleHandler createConsoleHandler(Level level) {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(level);
		handler.setFormatter(new ConsoleFormatter(anonymizePattern.get(), color.get()));
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

	public static Supplier<String> cause(String m, Throwable t) {
		return () -> getMessage(m, t);
	}

	public static Supplier<String> cause(Throwable t) {
		return () -> getMessage(null, t);
	}

	public static Supplier<String> message(Object... elements) {
		return () -> getMessage(elements);
	}

	private static String getMessage(String m, Throwable t) {
		// try to unravel stacked exceptions
		if (t instanceof RuntimeException && t.getCause() != null) {
			return getMessage(m, t.getCause());
		}

		// e.g. Failed to create file: AccessDeniedException: /path/to/file
		return getMessage(m, t.getClass().getSimpleName(), t.getMessage());
	}

	private static String getMessage(Object... elements) {
		return stream(elements).filter(Objects::nonNull).map(Objects::toString).collect(joining(": "));
	}

	public static class ConsoleFormatter extends Formatter {

		private final Pattern anonymize;
		private final boolean colorize;

		public ConsoleFormatter(Pattern anonymize, boolean colorize) {
			this.anonymize = anonymize;
			this.colorize = colorize;
		}

		@Override
		public String format(LogRecord record) {
			StringWriter buffer = new StringWriter();

			// BEGIN COLOR
			Color color = getColor(record.getLevel().intValue());
			if (color != null) {
				buffer.append(color.head);
			}

			// MESSAGE
			String message = record.getMessage();
			if (message != null && anonymize != null) {
				Matcher m = anonymize.matcher(message);
				while (m.find()) {
					m.appendReplacement(buffer.getBuffer(), "");
				}
				m.appendTail(buffer.getBuffer());
			} else {
				buffer.append(message);
			}

			// STACKTRACE
			Throwable thrown = record.getThrown();
			if (thrown != null) {
				buffer.append(System.lineSeparator());
				StackTraceUtils.deepSanitize(thrown).printStackTrace(new PrintWriter(buffer));
			}

			// END COLOR
			if (color != null) {
				buffer.append(color.tail);
			}

			return buffer.append(System.lineSeparator()).toString();
		}

		public Color getColor(int level) {
			if (colorize) {
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

			return null; // NO COLOR
		}

	}

	public static class ConsoleHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			// use either System.out or System.err depending on the severity of the error
			PrintStream out = record.getLevel().intValue() < Level.WARNING.intValue() ? System.out : System.err;

			// print messages to selected output stream
			out.print(getFormatter().format(record));

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

	public static class Color {

		public static final Color CHERRY_RED = new Color(0xC4);
		public static final Color ORANGE_RED = new Color(0xCA);
		public static final Color ROYAL_BLUE = new Color(0x28);
		public static final Color LIME_GREEN = new Color(0x27);

		public final String head;
		public final String tail;

		public Color(int color) {
			this("38;5;" + color); // see https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
		}

		public Color(String code) {
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
