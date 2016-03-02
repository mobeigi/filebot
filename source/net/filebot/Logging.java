package net.filebot;

import java.io.PrintStream;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.filebot.cli.CmdlineInterface;

import org.codehaus.groovy.runtime.StackTraceUtils;

public final class Logging {

	public static final Logger log = getConsoleLogger(CmdlineInterface.class, Level.ALL);
	public static final Logger debug = getConsoleLogger(Logging.class, Level.CONFIG);

	public static Logger getConsoleLogger(Class<?> cls, Level level) {
		Logger log = Logger.getLogger(cls.getPackage().getName());
		log.setLevel(level);
		log.setUseParentHandlers(false);
		log.addHandler(new ConsoleHandler());
		return log;
	}

	public static Supplier<String> format(String format, Object... args) {
		return () -> String.format(format, args);
	}

	private static final Pattern ANONYMIZE_PATTERN = getAnonymizePattern();

	private static Pattern getAnonymizePattern() {
		String pattern = System.getProperty("net.filebot.logging.anonymize");
		if (pattern != null && pattern.length() > 0) {
			return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.MULTILINE);
		}
		return null;
	}

	private static void printMessage(String message, PrintStream out) {
		if (message != null && message.length() > 0) {
			if (ANONYMIZE_PATTERN == null) {
				out.println(ANONYMIZE_PATTERN.matcher(message).replaceAll(""));
			} else {
				out.println(message);
			}
		}
	}

	private static void printStackTrace(Throwable throwable, PrintStream out) {
		if (throwable != null) {
			StackTraceUtils.deepSanitize(throwable).printStackTrace(out);
		}
	}

	public static class ConsoleHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			// use either System.out or System.err depending on the severity of the error
			PrintStream out = record.getLevel().intValue() < Level.WARNING.intValue() ? System.out : System.err;

			// print messages
			printMessage(record.getMessage(), out);
			printStackTrace(record.getThrown(), out);

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

}
