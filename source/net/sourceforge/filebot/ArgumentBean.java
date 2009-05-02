
package net.sourceforge.filebot;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.ui.transfer.FileTransferable;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;


public class ArgumentBean {
	
	@Option(name = "-help", usage = "Print this help message")
	private boolean help = false;
	
	@Option(name = "-clear", usage = "Clear history and settings")
	private boolean clear = false;
	
	@Option(name = "--sfv", usage = "Open file in 'SFV' panel", metaVar = "<file>")
	private boolean sfv;
	
	@Argument
	private List<File> arguments;
	
	
	public boolean help() {
		return help;
	}
	

	public boolean clear() {
		return clear;
	}
	

	public boolean sfv() {
		return sfv;
	}
	

	public List<File> arguments() {
		return arguments;
	}
	

	public FileTransferable transferable() {
		List<File> files = new ArrayList<File>(arguments.size());
		
		for (File argument : arguments) {
			if (argument.exists()) {
				try {
					// path may be relative, use absolute path
					files.add(argument.getCanonicalFile());
				} catch (IOException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
				}
			} else {
				// file doesn't exist
				Logger.getLogger(getClass().getName()).log(Level.WARNING, String.format("Invalid File: %s", argument));
			}
		}
		
		return new FileTransferable(files);
	}
	
}
