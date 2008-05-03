
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.FileBotUtil;


public class Unrar {
	
	private static final Command command = getCommand();
	
	private static final int timeout = 5000;
	private static final int sleepInterval = 50;
	
	
	public static void extractFiles(File archive, File destination) throws Exception {
		if (command == null) {
			throw new IllegalStateException("Unrar could not be initialized");
		}
		
		Process process = command.execute(archive, destination);
		
		int counter = 0;
		
		while (isRunning(process)) {
			Thread.sleep(sleepInterval);
			counter += sleepInterval;
			
			if (counter > timeout) {
				process.destroy();
				throw new TimeoutException(String.format("%s timed out", command.getName()));
			}
		}
		
		if (process.exitValue() != 0) {
			throw new Exception(String.format("%s returned with exit value %d", command.getName(), process.exitValue()));
		}
	}
	

	private static boolean isRunning(Process process) {
		try {
			// will throw exception if process is still running
			process.exitValue();
			
			// process has terminated
			return false;
		} catch (IllegalThreadStateException e) {
			// process is still running
			return true;
		}
	}
	

	private static Command getCommand() {
		try {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				File programFiles = new File(System.getenv("PROGRAMFILES"));
				
				for (File folder : programFiles.listFiles(FileBotUtil.FOLDERS_ONLY)) {
					String name = folder.getName().toLowerCase();
					
					if (name.contains("rar") || name.contains("zip")) {
						for (File file : folder.listFiles(FileBotUtil.FILES_ONLY)) {
							String filename = file.getName();
							
							if (filename.equalsIgnoreCase("unrar.exe") || filename.equalsIgnoreCase("7z.exe")) {
								return new Command(filename, file);
							}
						}
					}
				}
				
				throw new FileNotFoundException("External program not found");
			} else {
				String command = "unrar";
				
				// will throw an exception if command cannot be executed
				Runtime.getRuntime().exec(command);
				
				return new Command(command);
			}
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Cannot initialize unrar facility: " + e.getMessage());
		}
		
		return null;
	}
	
	
	private static class Command {
		
		private final String name;
		private final String executable;
		
		
		public Command(String command) {
			this.name = command;
			this.executable = command;
		}
		

		public Command(String name, File executable) {
			this.name = name;
			this.executable = executable.getAbsolutePath();
		}
		

		public Process execute(File archive, File workingDirectory) throws IOException {
			ProcessBuilder builder = new ProcessBuilder(executable, "x", "-y", archive.getAbsolutePath());
			builder.directory(workingDirectory);
			
			return builder.start();
		}
		

		public String getName() {
			return name;
		}
		
	}
}
