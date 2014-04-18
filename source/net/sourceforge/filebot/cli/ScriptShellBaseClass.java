package net.sourceforge.filebot.cli;

import static java.util.Collections.*;
import static java.util.EnumSet.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.filebot.util.StringUtilities.*;
import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.xml.MarkupBuilder;

import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import net.sourceforge.filebot.HistorySpooler;
import net.sourceforge.filebot.RenameAction;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.StandardRenameAction;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.AssociativeScriptObject;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.media.MetaAttributes;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.util.FileUtilities;
import net.sourceforge.filebot.web.Movie;

import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.sun.jna.Platform;

public abstract class ScriptShellBaseClass extends Script {

	private Map<String, Object> defaultValues;

	public void setDefaultValues(Map<String, ?> values) {
		this.defaultValues = new LinkedHashMap<String, Object>(values);
	}

	public Map<String, Object> getDefaultValues() {
		return defaultValues;
	}

	@Override
	public Object getProperty(String property) {
		try {
			return super.getProperty(property);
		} catch (MissingPropertyException e) {
			// try user-defined default values
			if (defaultValues != null && defaultValues.containsKey(property)) {
				return defaultValues.get(property);
			}

			// can't use default value, rethrow original exception
			throw e;
		}
	}

	public void include(String input) throws Throwable {
		try {
			executeScript(input, null);
		} catch (Exception e) {
			printException(e);
		}
	}

	public Object executeScript(String input) throws Throwable {
		return executeScript(input, null);
	}

	public Object executeScript(String input, Map<String, ?> bindings, Object... args) throws Throwable {
		// apply parent script defines
		Bindings parameters = new SimpleBindings();

		// initialize default parameter
		if (bindings != null) {
			parameters.putAll(bindings);
		}
		parameters.put(ScriptShell.ARGV_BINDING_NAME, FileUtilities.asFileList(args));

		// run given script
		ScriptShell shell = (ScriptShell) getBinding().getVariable(ScriptShell.SHELL_BINDING_NAME);
		return shell.runScript(input, parameters);
	}

	public Object tryQuietly(Closure<?> c) {
		try {
			return c.call();
		} catch (Exception e) {
			return null;
		}
	}

	public Object tryLogCatch(Closure<?> c) {
		try {
			return c.call();
		} catch (Exception e) {
			printException(e);
			return null;
		}
	}

	public void printException(Throwable t) {
		CLILogger.severe(String.format("%s: %s", t.getClass().getName(), t.getMessage()));

		// DEBUG
		StackTraceUtils.deepSanitize(t).printStackTrace();
	}

	public void die(String message) throws Throwable {
		throw new Exception(message);
	}

	// define global variable: _args
	public ArgumentBean get_args() {
		return getApplicationArguments();
	}

	// define global variable: _def
	public Map<String, String> get_def() {
		return unmodifiableMap(getApplicationArguments().defines);
	}

	// define global variable: _system
	public AssociativeScriptObject get_system() {
		return new AssociativeScriptObject(System.getProperties());
	}

	// define global variable: _environment
	public AssociativeScriptObject get_environment() {
		return new AssociativeScriptObject(System.getenv());
	}

	// Complete or session rename history
	public Map<File, File> getRenameLog() throws IOException {
		return getRenameLog(false);
	}

	public Map<File, File> getRenameLog(boolean complete) throws IOException {
		if (complete) {
			return HistorySpooler.getInstance().getCompleteHistory().getRenameMap();
		} else {
			return HistorySpooler.getInstance().getSessionHistory().getRenameMap();
		}
	}

	// define global variable: log
	public Logger getLog() {
		return CLILogger;
	}

	// define global variable: console
	public Console getConsole() {
		return System.console();
	}

	@Override
	public Object run() {
		return null;
	}

	public String detectSeriesName(Object files) throws Exception {
		return detectSeriesName(files, true, false);
	}

	public String detectAnimeName(Object files) throws Exception {
		return detectSeriesName(files, false, true);
	}

	public String detectSeriesName(Object files, boolean useSeriesIndex, boolean useAnimeIndex) throws Exception {
		List<String> names = MediaDetection.detectSeriesNames(FileUtilities.asFileList(files), useSeriesIndex, useAnimeIndex, Locale.ENGLISH);
		return names == null || names.isEmpty() ? null : names.get(0);
	}

	public static SxE parseEpisodeNumber(Object object) {
		List<SxE> matches = MediaDetection.parseEpisodeNumber(object.toString(), true);
		return matches == null || matches.isEmpty() ? null : matches.get(0);
	}

	public Movie detectMovie(File file, boolean strict) {
		// 1. xattr
		try {
			return (Movie) new MetaAttributes(file).getObject();
		} catch (Exception e) {
			// ignore and move on
		}

		// 2. perfect filename match
		try {
			List<String> names = new ArrayList<String>();
			for (File it : FileUtilities.listPathTail(file, 4, true)) {
				names.add(it.getName());
			}
			return MediaDetection.matchMovieName(names, true, 0).get(0);
		} catch (Exception e) {
			// ignore and move on
		}

		// 3. run full-fledged movie detection
		try {
			return MediaDetection.detectMovie(file, WebServices.OpenSubtitles, WebServices.TheMovieDB, Locale.ENGLISH, strict).get(0);
		} catch (Exception e) {
			// ignore and fail
		}

		return null;
	}

	public int execute(Object... args) throws Exception {
		List<String> cmd = new ArrayList<String>();

		if (Platform.isWindows()) {
			// normalize file separator for windows and run with cmd so any executable in PATH will just work
			cmd.add("cmd");
			cmd.add("/c");
		} else if (args.length == 1) {
			// make unix shell parse arguments
			cmd.add("sh");
			cmd.add("-c");
		}

		for (Object it : args) {
			cmd.add(it.toString());
		}

		ProcessBuilder process = new ProcessBuilder(cmd).inheritIO();
		return process.start().waitFor();
	}

	public String XML(Closure<?> buildClosure) {
		StringWriter out = new StringWriter();
		MarkupBuilder builder = new MarkupBuilder(out);
		buildClosure.rehydrate(buildClosure.getDelegate(), builder, builder).call(); // call closure in MarkupBuilder context
		return out.toString();
	}

	public void telnet(String host, int port, Closure<?> handler) throws IOException {
		try (Socket socket = new Socket(host, port)) {
			handler.call(new PrintStream(socket.getOutputStream(), true, "UTF-8"), new InputStreamReader(socket.getInputStream(), "UTF-8"));
		}
	}

	/**
	 * Retry given closure until it returns successfully (indefinitely if -1 is passed as retry count)
	 */
	public Object retry(int retryCountLimit, int retryWaitTime, Closure<?> c) throws InterruptedException {
		for (int i = 0; retryCountLimit < 0 || i <= retryCountLimit; i++) {
			try {
				return c.call();
			} catch (Exception e) {
				if (i >= 0 && i >= retryCountLimit) {
					throw e;
				}
				Thread.sleep(retryWaitTime);
			}
		}
		return null;
	}

	private enum Option {
		action, conflict, query, filter, format, db, order, lang, output, encoding, strict, forceExtractAll
	}

	private static final CmdlineInterface cli = new CmdlineOperations();

	public List<File> rename(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		Map<Option, Object> option = getDefaultOptions(parameters);
		RenameAction action = getRenameFunction(option.get(Option.action));
		boolean strict = DefaultTypeTransformation.castToBoolean(option.get(Option.strict));

		synchronized (cli) {
			try {
				return cli.rename(input, action, asString(option.get(Option.conflict)), asString(option.get(Option.output)), asString(option.get(Option.format)), asString(option.get(Option.db)), asString(option.get(Option.query)), asString(option.get(Option.order)), asString(option.get(Option.filter)), asString(option.get(Option.lang)), strict);
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	public List<File> getSubtitles(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		Map<Option, Object> option = getDefaultOptions(parameters);
		boolean strict = DefaultTypeTransformation.castToBoolean(option.get(Option.strict));

		synchronized (cli) {
			try {
				return cli.getSubtitles(input, asString(option.get(Option.db)), asString(option.get(Option.query)), asString(option.get(Option.lang)), asString(option.get(Option.output)), asString(option.get(Option.encoding)), asString(option.get(Option.format)), strict);
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	public List<File> getMissingSubtitles(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		Map<Option, Object> option = getDefaultOptions(parameters);
		boolean strict = DefaultTypeTransformation.castToBoolean(option.get(Option.strict));

		synchronized (cli) {
			try {
				return cli.getMissingSubtitles(input, asString(option.get(Option.db)), asString(option.get(Option.query)), asString(option.get(Option.lang)), asString(option.get(Option.output)), asString(option.get(Option.encoding)), asString(option.get(Option.format)), strict);
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	public boolean check(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);

		synchronized (cli) {
			try {
				return cli.check(input);
			} catch (Exception e) {
				printException(e);
				return false;
			}
		}
	}

	public File compute(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		Map<Option, Object> option = getDefaultOptions(parameters);

		synchronized (cli) {
			try {
				return cli.compute(input, asString(option.get(Option.output)), asString(option.get(Option.encoding)));
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	public List<File> extract(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		Map<Option, Object> option = getDefaultOptions(parameters);
		FileFilter filter = (FileFilter) DefaultTypeTransformation.castToType(option.get(Option.filter), FileFilter.class);
		boolean forceExtractAll = DefaultTypeTransformation.castToBoolean(option.get(Option.forceExtractAll));

		synchronized (cli) {
			try {
				return cli.extract(input, asString(option.get(Option.output)), asString(option.get(Option.conflict)), filter, forceExtractAll);
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	public List<String> fetchEpisodeList(Map<String, ?> parameters) throws Exception {
		Map<Option, Object> option = getDefaultOptions(parameters);

		synchronized (cli) {
			try {
				return cli.fetchEpisodeList(asString(option.get(Option.query)), asString(option.get(Option.format)), asString(option.get(Option.db)), asString(option.get(Option.order)), asString(option.get(Option.lang)));
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	public String getMediaInfo(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		Map<Option, Object> option = getDefaultOptions(parameters);
		synchronized (cli) {
			try {
				return cli.getMediaInfo(input.get(0), asString(option.get(Option.format)));
			} catch (Exception e) {
				printException(e);
				return null;
			}
		}
	}

	private List<File> getInputFileList(Map<String, ?> map) {
		Object file = map.get("file");
		if (file != null) {
			return FileUtilities.asFileList(file);
		}

		Object folder = map.get("folder");
		if (folder != null) {
			return FileUtilities.listFiles(FileUtilities.asFileList(folder), 0, false, true, false);
		}

		throw new IllegalArgumentException("file is not set");
	}

	private Map<Option, Object> getDefaultOptions(Map<String, ?> parameters) throws Exception {
		Map<Option, Object> options = new EnumMap<Option, Object>(Option.class);

		for (Entry<String, ?> it : parameters.entrySet()) {
			try {
				options.put(Option.valueOf(it.getKey()), it.getValue());
			} catch (IllegalArgumentException e) {
				// just ignore illegal options
			}
		}

		ArgumentBean defaultValues = Settings.getApplicationArguments();
		for (Option missing : complementOf(copyOf(options.keySet()))) {
			switch (missing) {
			case forceExtractAll:
				options.put(missing, false);
				break;
			case strict:
				options.put(missing, !defaultValues.nonStrict);
				break;
			default:
				options.put(missing, defaultValues.getClass().getField(missing.name()).get(defaultValues));
				break;
			}
		}

		return options;
	}

	private RenameAction getRenameFunction(final Object obj) {
		if (obj instanceof RenameAction) {
			return (RenameAction) obj;
		}
		if (obj instanceof CharSequence) {
			return StandardRenameAction.forName(obj.toString());
		}
		if (obj instanceof Closure<?>) {
			return new RenameAction() {

				private final Closure<?> closure = (Closure<?>) obj;

				@Override
				public File rename(File from, File to) throws Exception {
					Object value = closure.call(from, to);

					// must return File object, so we try the result of the closure, but if it's not a File we just return the original destination parameter
					return value instanceof File ? (File) value : to;
				}

				@Override
				public String toString() {
					return "CLOSURE";
				}
			};
		}

		// object probably can't be casted
		return (RenameAction) DefaultTypeTransformation.castToType(obj, RenameAction.class);
	}

}
