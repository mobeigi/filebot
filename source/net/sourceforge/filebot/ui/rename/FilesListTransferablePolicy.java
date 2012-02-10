
package net.sourceforge.filebot.ui.rename;


import static java.util.Arrays.*;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.filebot.media.MediaDetection;
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
		List<File> entries = new ArrayList<File>();
		LinkedList<File> queue = new LinkedList<File>(files);
		
		while (queue.size() > 0) {
			File f = queue.removeFirst();
			
			if (f.isHidden())
				continue;
			
			if (f.isFile() || MediaDetection.isDiskFolder(f)) {
				entries.add(f);
			} else {
				queue.addAll(0, asList(f.listFiles()));
			}
		}
		
		model.addAll(FastFile.foreach(entries));
	}
	
	
	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}
	
}
