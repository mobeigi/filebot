
package net.sourceforge.filebot.ui.panel.rename.entry;


import java.io.File;

import net.sourceforge.filebot.FileFormat;


public class FileEntry extends AbstractFileEntry<File> {
	
	public FileEntry(File file) {
		super(file);
	}
	

	@Override
	public String getName(File value) {
		return FileFormat.getNameWithoutSuffix(getValue());
	}
	

	@Override
	public long getLength() {
		return getValue().length();
	}
	
}
