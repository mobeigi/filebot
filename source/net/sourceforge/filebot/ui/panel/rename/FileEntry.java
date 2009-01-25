
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;

import net.sourceforge.tuned.FileUtilities;


class FileEntry extends AbstractFileEntry {
	
	private final File file;
	private final String type;
	
	
	public FileEntry(File file) {
		super(FileUtilities.getName(file), file.length());
		
		this.file = file;
		this.type = FileUtilities.getType(file);
	}
	

	public File getFile() {
		return file;
	}
	

	public String getType() {
		return type;
	}
	
}
