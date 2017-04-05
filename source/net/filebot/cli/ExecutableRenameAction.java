package net.filebot.cli;

import java.io.File;
import java.io.IOException;

import net.filebot.RenameAction;

public class ExecutableRenameAction implements RenameAction {

	private final String executable;

	public ExecutableRenameAction(String executable) {
		this.executable = executable;
	}

	@Override
	public File rename(File from, File to) throws Exception {
		ProcessBuilder process = new ProcessBuilder(executable, from.getPath(), to.getPath());
		process.directory(from.getParentFile());
		process.inheritIO();

		int exitCode = process.start().waitFor();
		if (exitCode != 0) {
			throw new IOException(String.format("%s failed with exit code %d", process.command(), exitCode));
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
