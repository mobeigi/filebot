
package net.sourceforge.filebot.ui.panel.rename.entry;


import java.io.File;

import net.sourceforge.filebot.FileFormat;


public class FileEntry extends AbstractFileEntry<File> {
	
	public FileEntry(File file) {
		super(FileFormat.getFileName(file), file, FileFormat.getFileType(file), file.length());
	}
	
}
