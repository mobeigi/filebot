
package net.sourceforge.filebot.ui.panel.rename;


import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;


public class FilesRenameList extends RenameList {
	
	public FilesRenameList() {
		setTitle("Files");
		setTransferablePolicy(new FilesRenameListTransferablePolicy(this.getModel()));
	}
	

	public List<FileEntry> getListEntries() {
		DefaultListModel model = getModel();
		
		List<FileEntry> files = new ArrayList<FileEntry>();
		
		for (int i = 0; i < model.getSize(); ++i)
			files.add((FileEntry) model.get(i));
		
		return files;
	}
	
}
