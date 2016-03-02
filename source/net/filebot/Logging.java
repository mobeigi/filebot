package net.filebot;

import java.io.PrintStream;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

import net.filebot.cli.CmdlineInterface;

import org.codehaus.groovy.runtime.StackTraceUtils;

public final class Logging {

	public static final Logger log = getConsoleLogger(CmdlineInterface.class, Level.ALL, new ConsoleFormatter());
	public static final Logger debug = getConsoleLogger(Logging.class, Level.ALL, new SimpleFormatter());

	public static Logger getConsoleLogger(Class<?> cls, Level level, Formatter formatter) {
		Logger log = Logger.getLogger(cls.getPackage().getName());
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

		@Override
		public String format(LogRecord record) {
			if (ANONYMIZE_PATTERN != null) {
				return ANONYMIZE_PATTERN.matcher(record.getMessage()).replaceAll("") + System.lineSeparator();
			}
			return record.getMessage() + System.lineSeparator();
		}

	}

	private static final Pattern ANONYMIZE_PATTERN = getAnonymizePattern();

	private static Pattern getAnonymizePattern() {
		String pattern = System.getProperty("net.filebot.logging.anonymize");
		if (pattern != null && pattern.length() > 0) {
			return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.MULTILINE);
		}
		return null;
	}

}
