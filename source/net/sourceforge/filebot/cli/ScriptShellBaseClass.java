package net.sourceforge.filebot.cli;

import static java.util.Collections.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.io.Console;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.format.AssociativeScriptObject;

public abstract class ScriptShellBaseClass extends Script {

	public ScriptShellBaseClass() {
		System.out.println(this);
	}

	public Object executeScript(String input, Map<String, ?> bindings, Object... args) throws Throwable {
		// apply parent script defines
		Bindings parameters = new SimpleBindings();

		// initialize default parameter
		parameters.putAll(bindings);
		parameters.put(ScriptShell.ARGV_BINDING_NAME, asFileList(args));

		// run given script
		ScriptShell shell = (ScriptShell) getBinding().getVariable(ScriptShell.SHELL_BINDING_NAME);
		return shell.runScript(input, parameters);
	}

	private Map<String, ?> defaultValues;

	public void setDefaultValues(Map<String, ?> values) {
		this.defaultValues = values;
	}

	public Map<String, ?> getDefaultValues() {
		return defaultValues == null ? null : unmodifiableMap(defaultValues);
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
			CLILogger.severe(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
			return null;
		}
	}

	@Override
	public Object run() {
		return null;
	}

	// define global variable: _args
	public ArgumentBean get_args() {
		return getApplicationArguments();
	}

	// define global variable: _def
	public Map<String, String> get_def() {
		return getApplicationArguments().defines;
	}

	// define global variable: _system
	public AssociativeScriptObject get_system() {
		return new AssociativeScriptObject(System.getProperties());
	}

	// define global variable: _environment
	public AssociativeScriptObject get_environment() {
		return new AssociativeScriptObject(System.getenv());
	}

	// define global variable: _types
	public MediaTypes get_types() {
		return MediaTypes.getDefault();
	}

	// define global variable: _log
	public Logger get_log() {
		return CLILogger;
	}

	// define global variable: console
	public Console getConsole() {
		return System.console();
	}

	public static List<File> asFileList(Object... paths) {
		List<File> files = new ArrayList<File>();
		for (Object it : paths) {
			if (it instanceof CharSequence) {
				files.add(new File(it.toString()));
			} else if (it instanceof File) {
				files.add((File) it);
			} else if (it instanceof Path) {
				files.add(((Path) it).toFile());
			}
		}
		return files;
	}

}
