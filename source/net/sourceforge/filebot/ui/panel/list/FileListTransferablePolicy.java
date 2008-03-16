
package net.sourceforge.filebot.ui.panel.list;


import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;


class FileListTransferablePolicy extends FileTransferablePolicy {
	
	private FileBotList list;
	
	
	public FileListTransferablePolicy(FileBotList list) {
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
			
			for (File f : file.listFiles()) {
				list.getModel().add(FileFormat.formatName(f));
			}
		} else {
			if (FileFormat.getSuffix(file).equalsIgnoreCase("torrent")) {
				try {
					Torrent torrent = new Torrent(file);
					list.setTitle(FileFormat.getNameWithoutSuffix(torrent.getName()));
					
					for (Torrent.Entry entry : torrent.getFiles()) {
						list.getModel().add(FileFormat.getNameWithoutSuffix(entry.getName()));
					}
				} catch (IOException e) {
					// should not happen
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
				}
			}
		}
	}
	

	@Override
	public String getDescription() {
		return "folders and torrents";
	}
	
}
