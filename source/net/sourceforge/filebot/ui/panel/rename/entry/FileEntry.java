
package net.sourceforge.filebot.ui.panel.rename.entry;


import java.io.File;

import net.sourceforge.filebot.FileFormat;


public class FileEntry extends AbstractFileEntry<File> {
	
	private final long length;
	private final String type;
	
	
	public FileEntry(File file) {
		super(FileFormat.getFileName(file), file);
		
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
}
