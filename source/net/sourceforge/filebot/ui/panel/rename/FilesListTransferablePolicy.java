
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.tuned.FileUtilities.FOLDERS;
import static net.sourceforge.tuned.FileUtilities.containsOnly;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import net.sourceforge.tuned.FastFile;


class FilesListTransferablePolicy extends FileTransferablePolicy {
	
	private final List<File> model;
	
	
	public FilesListTransferablePolicy(List<File> model) {
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
		if (containsOnly(files, FOLDERS)) {
			for (File folder : files) {
				loadFiles(Arrays.asList(folder.listFiles()));
			}
		} else {
			loadFiles(files);
		}
	}
	

	protected void loadFiles(List<File> files) {
		for (File file : files) {
			model.add(new FastFile(file.getPath()));
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}
	
}
