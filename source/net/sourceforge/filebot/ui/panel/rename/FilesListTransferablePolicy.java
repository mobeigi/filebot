
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import ca.odell.glazedlists.EventList;


class FilesListTransferablePolicy extends FileTransferablePolicy {
	
	private final EventList<? super FileEntry> model;
	
	
	public FilesListTransferablePolicy(EventList<? super FileEntry> model) {
		this.model = model;
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	protected void clear() {
		model.clear();
	}
	

	@Override
	protected void load(List<File> files) {
		if (FileBotUtil.containsOnlyFolders(files)) {
			for (File folder : files) {
				loadFiles(Arrays.asList(folder.listFiles()));
			}
		} else {
			loadFiles(files);
		}
	}
	

	protected void loadFiles(List<File> files) {
		for (File file : files) {
			model.add(new FileEntry(file));
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}
	
}
