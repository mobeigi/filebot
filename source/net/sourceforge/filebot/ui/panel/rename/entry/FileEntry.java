
package net.sourceforge.filebot.ui.panel.rename.entry;


import java.io.File;

import net.sourceforge.filebot.FileFormat;


public class FileEntry extends AbstractFileEntry {
	
	private final File file;
	
	private final long length;
	private final String type;
	
	
	public FileEntry(File file) {
		super(FileFormat.getFileName(file));
		
		this.file = file;
		this.length = file.length();
		this.type = FileFormat.getFileType(file);
	}
	

	@Override
	public long getLength() {
		return length;
	}
	

	public String getType() {
		return type;
	}
	

	public File getFile() {
		return file;
	}
}
