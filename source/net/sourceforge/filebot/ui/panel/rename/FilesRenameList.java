
package net.sourceforge.filebot.ui.panel.rename;


import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;


class FilesRenameList extends RenameList {
	
	public FilesRenameList() {
		setTitle("Files");
		setTransferablePolicy(new FilesListTransferablePolicy(getModel()));
	}
	

	@SuppressWarnings("unchecked")
	public List<FileEntry> getListEntries() {
		return (List<FileEntry>) getModel().getCopy();
	}
	
}
