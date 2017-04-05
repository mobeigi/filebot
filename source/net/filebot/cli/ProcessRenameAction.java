package net.filebot.cli;

import static net.filebot.Logging.*;

import java.io.File;

import net.filebot.RenameAction;

public class ProcessRenameAction implements RenameAction {

	private final String executable;

	public ProcessRenameAction(String executable) {
		this.executable = executable;
	}

	@Override
	public File rename(File from, File to) throws Exception {
		Process process = new ProcessBuilder(executable, from.getPath(), to.getPath()).inheritIO().start();

		if (process.waitFor() != 0) {
			debug.severe(format("[%s] failed with exit code %d", executable, process.exitValue()));
		}

		return null;
	}

	@Override
	public boolean canRevert() {
		return false;
	}

	@Override
	public String toString() {
		return executable;
	}

}
