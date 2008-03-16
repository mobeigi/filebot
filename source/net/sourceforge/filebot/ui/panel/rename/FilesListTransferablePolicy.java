
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.Arrays;

import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;
import net.sourceforge.tuned.ui.SimpleListModel;


class FilesListTransferablePolicy extends FileTransferablePolicy {
	
	private SimpleListModel listModel;
	
	
	public FilesListTransferablePolicy(SimpleListModel listModel) {
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
	protected void load(File file) {
		if (file.isDirectory()) {
			File subfiles[] = file.listFiles();
			Arrays.sort(subfiles);
			
			for (File f : subfiles)
				listModel.add(new FileEntry(f));
		} else {
			listModel.add(new FileEntry(file));
		}
	}
	

	@Override
	public String getDescription() {
		return "files and folders";
	}
	
}
