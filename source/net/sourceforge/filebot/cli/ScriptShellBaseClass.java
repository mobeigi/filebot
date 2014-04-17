package net.sourceforge.filebot.cli;

import static java.util.Collections.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.xml.MarkupBuilder;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import net.sourceforge.filebot.HistorySpooler;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.AssociativeScriptObject;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.media.MetaAttributes;
import net.sourceforge.filebot.util.FileUtilities;
import net.sourceforge.filebot.web.Movie;

import com.sun.jna.Platform;

public abstract class ScriptShellBaseClass extends Script {

	public ScriptShellBaseClass() {
		System.out.println(this);
	}

	private Map<String, ?> defaultValues;

	public void setDefaultValues(Map<String, ?> values) {
		this.defaultValues = values;
	}

	public Map<String, ?> getDefaultValues() {
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

			// can't use default value, rethrow exception
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

	public Object tryLoudly(Closure<?> c) {
		try {
			return c.call();
		} catch (Exception e) {
			printException(e);
			return null;
		}
	}

	public void printException(Throwable t) {
		CLILogger.severe(String.format("%s: %s", t.getClass().getSimpleName(), t.getMessage()));
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
		List<String> names = MediaDetection.detectSeriesNames(FileUtilities.asFileList(files), true, false, Locale.ENGLISH);
		return names.isEmpty() ? null : names.get(0);
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

	private enum OptionName {
		action, conflict, query, filter, format, db, order, lang, output, encoding, strict
	}

	private Map<OptionName, Object> withDefaultOptions(Map<String, ?> map) throws Exception {
		Map<OptionName, Object> options = new EnumMap<OptionName, Object>(OptionName.class);

		for (Entry<String, ?> it : map.entrySet()) {
			options.put(OptionName.valueOf(it.getKey()), it.getValue());
		}

		ArgumentBean defaultValues = Settings.getApplicationArguments();
		for (OptionName missing : EnumSet.complementOf(EnumSet.copyOf(options.keySet()))) {
			if (missing == OptionName.strict) {
				options.put(missing, !defaultValues.nonStrict);
			} else {
				Object value = defaultValues.getClass().getField(missing.name()).get(defaultValues);
				options.put(missing, value);
			}
		}

		return options;
	}
}
