
package net.sourceforge.filebot.ui.panel.rename;


import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;


public class FilesRenameList extends RenameList {
	
	public FilesRenameList() {
		setTitle("Files");
		setTransferablePolicy(new FilesRenameListTransferablePolicy(getModel()));
	}
	

	@SuppressWarnings("unchecked")
	public List<FileEntry> getListEntries() {
		return (List<FileEntry>) getModel().getCopy();
	}
	
}
