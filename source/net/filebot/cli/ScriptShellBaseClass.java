package net.filebot.cli;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.Logging.*;
import static net.filebot.media.XattrMetaInfo.*;
import static net.filebot.util.StringUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.sun.jna.Platform;

import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.xml.MarkupBuilder;
import net.filebot.HistorySpooler;
import net.filebot.RenameAction;
import net.filebot.StandardRenameAction;
import net.filebot.WebServices;
import net.filebot.format.AssociativeScriptObject;
import net.filebot.media.MediaDetection;
import net.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.filebot.util.FileUtilities;
import net.filebot.web.Movie;

public abstract class ScriptShellBaseClass extends Script {

	private final Map<String, Object> defaultValues = synchronizedMap(new LinkedHashMap<String, Object>());

	public void setDefaultValues(Map<String, ?> values) {
		defaultValues.putAll(values);
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
			executeScript(input, null, null, null);
		} catch (Exception e) {
			printException(e, true);
		}
	}

	public Object runScript(String input, String... argv) throws Throwable {
		try {
			ArgumentBean args = argv == null || argv.length == 0 ? getArgumentBean() : ArgumentBean.parse(argv);
			return executeScript(input, asList(getArgumentBean().getArray()), args.defines, args.getFiles(false));
		} catch (Exception e) {
			printException(e, true);
		}
		return null;
	}

	public Object executeScript(String input, Map<String, ?> bindings, Object... args) throws Throwable {
		return executeScript(input, asList(getArgumentBean().getArray()), bindings, FileUtilities.asFileList(args));
	}

	public Object executeScript(String input, List<String> argv, Map<String, ?> bindings, List<File> args) throws Throwable {
		// apply parent script defines
		Bindings parameters = new SimpleBindings();

		// initialize default parameter
		if (bindings != null) {
			parameters.putAll(bindings);
		}

		parameters.put(ScriptShell.SHELL_ARGV_BINDING_NAME, argv != null ? argv.toArray(new String[0]) : new String[0]);
		parameters.put(ScriptShell.ARGV_BINDING_NAME, args != null ? new ArrayList<File>(args) : new ArrayList<File>());

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
			printException(e, false);
			return null;
		}
	}

	public void printException(Throwable t) {
		printException(t, false);
	}

	public void printException(Throwable t, boolean severe) {
		Level level = severe ? Level.SEVERE : Level.WARNING;

		// print full stacktrace in debug mode
		if (debug.isLoggable(level)) {
			debug.log(level, t, t::getMessage);
			return;
		}

		if (severe) {
			log.log(level, trace(t));
		} else {
			log.log(level, t, t::getMessage);
		}
	}

	public void die(Object cause) throws Throwable {
		if (cause instanceof Throwable) {
			throw new ScriptDeath((Throwable) cause);
		}
		throw new ScriptDeath(String.valueOf(cause));
	}

	// define global variable: _args
	public ArgumentBean get_args() {
		return getArgumentBean();
	}

	// define global variable: _def
	public Map<String, String> get_def() {
		return getArgumentBean().defines;
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
		return log;
	}

	// define global variable: console
	public Object getConsole() {
		return System.console() != null ? System.console() : PseudoConsole.getSystemConsole();
	}

	public Date getNow() {
		return new Date();
	}

	@Override
	public Object run() {
		return null;
	}

	public String detectSeriesName(Object files) throws Exception {
		return detectSeriesName(files, false);
	}

	public String detectAnimeName(Object files) throws Exception {
		return detectSeriesName(files, true);
	}

	public String detectSeriesName(Object files, boolean anime) throws Exception {
		List<File> input = FileUtilities.asFileList(files);
		if (input.isEmpty())
			return null;

		List<String> names = MediaDetection.detectSeriesNames(input, anime, Locale.ENGLISH);
		return names == null || names.isEmpty() ? null : names.get(0);
	}

	public static SxE parseEpisodeNumber(Object object) {
		List<SxE> matches = MediaDetection.parseEpisodeNumber(object.toString(), true);
		return matches == null || matches.isEmpty() ? null : matches.get(0);
	}

	public Movie detectMovie(File file, boolean strict) {
		// 1. xattr
		Object metaObject = xattr.getMetaInfo(file);
		if (metaObject instanceof Movie) {
			return (Movie) metaObject;
		}

		// 2. perfect filename match
		try {
			Movie match = MediaDetection.matchMovie(file, 4);
			if (match != null) {
				return match;
			}
		} catch (Exception e) {
			// ignore and move on
		}

		// 3. run full-fledged movie detection
		try {
			return MediaDetection.detectMovie(file, WebServices.TheMovieDB, Locale.ENGLISH, strict).get(0);
		} catch (Exception e) {
			// ignore and fail
		}

		return null;
	}

	public Movie matchMovie(String name) {
		List<Movie> matches = MediaDetection.matchMovieName(singleton(name), true, 0);
		return matches == null || matches.isEmpty() ? null : matches.get(0);
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
				if (input.isEmpty() && !getInputFileMap(parameters).isEmpty()) {
					return cli.rename(getInputFileMap(parameters), action, asString(option.get(Option.conflict)));
				}

				return cli.rename(input, action, asString(option.get(Option.conflict)), asString(option.get(Option.output)), asString(option.get(Option.format)), asString(option.get(Option.db)), asString(option.get(Option.query)), asString(option.get(Option.order)), asString(option.get(Option.filter)), asString(option.get(Option.lang)), strict);
			} catch (Exception e) {
				printException(e, false);
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
				printException(e, false);
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
				printException(e, false);
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
				printException(e, false);
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
				printException(e, false);
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
				printException(e, false);
				return null;
			}
		}
	}

	public List<String> fetchEpisodeList(Map<String, ?> parameters) throws Exception {
		Map<Option, Object> option = getDefaultOptions(parameters);

		synchronized (cli) {
			try {
				return cli.fetchEpisodeList(asString(option.get(Option.query)), asString(option.get(Option.format)), asString(option.get(Option.db)), asString(option.get(Option.order)), asString(option.get(Option.filter)), asString(option.get(Option.lang)));
			} catch (Exception e) {
				printException(e, false);
				return null;
			}
		}
	}

	public String getMediaInfo(File file, String format) throws Exception {
		return cli.getMediaInfo(singleton(file), format, null).get(0); // explicitly ignore the --filter option
	}

	public List<String> getMediaInfo(Collection<?> files, String format) throws Exception {
		return cli.getMediaInfo(FileUtilities.asFileList(files), format, null); // explicitly ignore the --filter option
	}

	public Object getMediaInfo(Map<String, ?> parameters) throws Exception {
		List<File> input = getInputFileList(parameters);
		if (input == null || input.isEmpty()) {
			return null;
		}

		Map<Option, Object> option = getDefaultOptions(parameters);
		synchronized (cli) {
			try {
				List<String> lines = cli.getMediaInfo(input, asString(option.get(Option.format)), asString(option.get(Option.filter)));
				if (parameters.containsKey("file") && !(parameters.get("file") instanceof Collection)) {
					return lines.get(0); // HACK for script backwards compatibility
				} else {
					return lines;
				}
			} catch (Exception e) {
				printException(e, false);
				return null;
			}
		}
	}

	private List<File> getInputFileList(Map<String, ?> parameters) {
		Object file = parameters.get("file");
		if (file != null) {
			return FileUtilities.asFileList(file);
		}

		Object folder = parameters.get("folder");
		if (folder != null) {
			return FileUtilities.listFiles(FileUtilities.asFileList(folder), 0, false, true, false);
		}

		return emptyList();
	}

	private Map<File, File> getInputFileMap(Map<String, ?> parameters) {
		Map<?, ?> map = (Map<?, ?>) parameters.get("map");
		Map<File, File> files = new LinkedHashMap<File, File>();
		if (map != null) {
			for (Entry<?, ?> it : map.entrySet()) {
				List<File> key = FileUtilities.asFileList(it.getKey());
				List<File> value = FileUtilities.asFileList(it.getValue());
				if (key.size() == 1 && value.size() == 1) {
					files.put(key.get(0), value.get(0));
				} else {
					throw new IllegalArgumentException("Illegal file mapping: " + it);
				}
			}
		}
		return files;
	}

	private ArgumentBean getArgumentBean() {
		try {
			return ArgumentBean.parse((String[]) getBinding().getVariable(ScriptShell.SHELL_ARGV_BINDING_NAME));
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage());
		}
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

		ArgumentBean args = getArgumentBean();
		Set<Option> complement = EnumSet.allOf(Option.class);
		complement.removeAll(options.keySet());

		for (Option missing : complement) {
			switch (missing) {
			case forceExtractAll:
				options.put(missing, false);
				break;
			case strict:
				options.put(missing, !args.nonStrict);
				break;
			default:
				options.put(missing, args.getClass().getField(missing.name()).get(args));
				break;
			}
		}

		return options;
	}

	public RenameAction getRenameFunction(final Object obj) {
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
