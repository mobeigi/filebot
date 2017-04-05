package net.filebot.cli;

import java.io.File;

import groovy.lang.Closure;
import net.filebot.RenameAction;

public class GroovyRenameAction implements RenameAction {

	private final Closure<?> closure;

	public GroovyRenameAction(Closure<?> closure) {
		this.closure = closure;
	}

	@Override
	public File rename(File from, File to) throws Exception {
		Object value = closure.call(from, to);

		// must return File object, so we try the result of the closure, but if it's not a File we just return the original destination parameter
		return value instanceof File ? (File) value : null;
	}

	@Override
	public boolean canRevert() {
		return false;
	}

	@Override
	public String toString() {
		return "CLOSURE";
	}

}
