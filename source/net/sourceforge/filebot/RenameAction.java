
package net.sourceforge.filebot;


import java.io.File;


public interface RenameAction {
	
	File rename(File from, File to) throws Exception;
	
}
