
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.Arrays;

import javax.swing.DefaultListModel;

import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;


public class FilesRenameListTransferablePolicy extends FileTransferablePolicy {
	
	private DefaultListModel listModel;
	
	
	public FilesRenameListTransferablePolicy(DefaultListModel listModel) {
		this.listModel = listModel;
	}
	

	@Override
	protected boolean accept(File file) {
		return file.isDirectory() || file.isFile();
	}
	

	@Override
	protected void clear() {
		listModel.clear();
	}
	

	@Override
	protected boolean load(File file) {
		if (file.isDirectory()) {
			File subfiles[] = file.listFiles();
			Arrays.sort(subfiles);
			
			for (File f : subfiles)
				listModel.addElement(new FileEntry(f));
		} else
			listModel.addElement(new FileEntry(file));
		
		return true;
	}
	

	@Override
	public String getDescription() {
		return "files and folders";
	}
	
}
