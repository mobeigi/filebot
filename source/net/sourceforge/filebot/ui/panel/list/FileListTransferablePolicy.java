
package net.sourceforge.filebot.ui.panel.list;


import static net.sourceforge.filebot.FileBotUtil.TORRENT_FILE_EXTENSIONS;
import static net.sourceforge.filebot.FileBotUtil.containsOnly;
import static net.sourceforge.filebot.FileBotUtil.containsOnlyFolders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import net.sourceforge.tuned.FileUtil;


class FileListTransferablePolicy extends FileTransferablePolicy {
	
	private FileBotList<? super String> list;
	
	
	public FileListTransferablePolicy(FileBotList<? super String> list) {
		this.list = list;
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	protected void clear() {
		list.getModel().clear();
	}
	

	@Override
	protected void load(List<File> files) {
		// set title based on parent folder of first file
		list.setTitle(FileUtil.getFolderName(files.get(0).getParentFile()));
		
		if (containsOnlyFolders(files)) {
			loadFolders(files);
		} else if (containsOnly(files, TORRENT_FILE_EXTENSIONS)) {
			loadTorrents(files);
		} else {
			for (File file : files) {
				list.getModel().add(FileUtil.getFileName(file));
			}
		}
	}
	

	private void loadFolders(List<File> folders) {
		if (folders.size() == 1) {
			// if only one folder was dropped, use its name as title
			list.setTitle(FileUtil.getFolderName(folders.get(0)));
		}
		
		for (File folder : folders) {
			for (File file : folder.listFiles()) {
				list.getModel().add(FileUtil.getFileName(file));
			}
		}
	}
	

	private void loadTorrents(List<File> torrentFiles) {
		try {
			List<Torrent> torrents = new ArrayList<Torrent>(torrentFiles.size());
			
			for (File file : torrentFiles) {
				torrents.add(new Torrent(file));
			}
			
			if (torrentFiles.size() == 1) {
				list.setTitle(FileUtil.getNameWithoutExtension(torrents.get(0).getName()));
			}
			
			for (Torrent torrent : torrents) {
				for (Torrent.Entry entry : torrent.getFiles()) {
					list.getModel().add(FileUtil.getNameWithoutExtension(entry.getName()));
				}
			}
		} catch (IOException e) {
			// should not happen
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files, folders and torrents";
	}
	
}
