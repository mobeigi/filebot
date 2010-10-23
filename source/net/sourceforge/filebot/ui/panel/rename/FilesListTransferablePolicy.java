
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
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
		model.addAll(FastFile.foreach(flatten(files, 5)));
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}
	
}
