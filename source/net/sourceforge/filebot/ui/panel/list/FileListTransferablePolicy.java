
package net.sourceforge.filebot.ui.panel.list;


import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.FileFormat;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;


public class FileListTransferablePolicy extends FileTransferablePolicy {
	
	private FileList list;
	
	
	public FileListTransferablePolicy(FileList list) {
		this.list = list;
	}
	

	@Override
	protected boolean accept(File file) {
		return file.isDirectory() || FileFormat.getSuffix(file).equalsIgnoreCase("torrent");
	}
	

	@Override
	protected void clear() {
		list.getModel().clear();
	}
	

	@Override
	protected void load(File file) {
		if (file.isDirectory()) {
			list.setTitle(file.getName());
			
			for (File f : file.listFiles())
				list.getModel().addElement(FileFormat.formatName(f));
		} else {
			if (FileFormat.getSuffix(file).equalsIgnoreCase("torrent")) {
				try {
					Torrent torrent = new Torrent(file);
					list.setTitle(FileFormat.getNameWithoutSuffix(torrent.getName()));
					
					for (Torrent.Entry entry : torrent.getFiles()) {
						list.getModel().addElement(FileFormat.getNameWithoutSuffix(entry.getName()));
					}
				} catch (IOException e) {
					// should not happen
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
				}
			}
		}
	}
	

	@Override
	public String getDescription() {
		return "folders and torrents";
	}
	
}
