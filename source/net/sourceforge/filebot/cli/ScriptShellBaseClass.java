package net.sourceforge.filebot.cli;

import static net.sourceforge.filebot.cli.CLILogging.*;
import groovy.lang.Closure;
import groovy.lang.Script;

import java.io.Console;
import java.util.logging.Logger;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.format.AssociativeScriptObject;

public abstract class ScriptShellBaseClass extends Script {

	public ScriptShellBaseClass() {
		System.out.println(this);
	}

	public Object _guarded(Closure<?> c) {
		try {
			return c.call();
		} catch (Throwable e) {
			CLILogger.severe(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
			return null;
		}
	}

	@Override
	public Object run() {
		return null;
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

}
