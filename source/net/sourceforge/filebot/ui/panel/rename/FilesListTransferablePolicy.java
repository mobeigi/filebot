
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;
import net.sourceforge.tuned.ui.SimpleListModel;


class FilesListTransferablePolicy extends FileTransferablePolicy {
	
	private final SimpleListModel model;
	
	
	public FilesListTransferablePolicy(SimpleListModel listModel) {
		this.model = listModel;
	}
	

	@Override
	protected boolean accept(File file) {
		return file.isFile() || file.isDirectory();
	}
	

	@Override
	protected void clear() {
		model.clear();
	}
	

	@Override
	protected void load(List<File> files) {
		if (FileBotUtil.containsOnlyFolders(files)) {
			for (File folder : files) {
				super.load(Arrays.asList(folder.listFiles()));
			}
		} else {
			super.load(files);
		}
	}
	

	@Override
	protected void load(File file) {
		model.add(new FileEntry(file));
	}
	

	@Override
	public String getDescription() {
		return "files and folders";
	}
	
}
