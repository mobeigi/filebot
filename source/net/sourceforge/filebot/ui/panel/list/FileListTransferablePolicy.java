
package net.sourceforge.filebot.ui.panel.list;


import static net.sourceforge.filebot.FileBotUtilities.TORRENT_FILES;
import static net.sourceforge.tuned.FileUtilities.FOLDERS;
import static net.sourceforge.tuned.FileUtilities.containsOnly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import net.sourceforge.tuned.FileUtilities;


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
	protected void load(List<File> files) throws IOException {
		// set title based on parent folder of first file
		list.setTitle(FileUtilities.getFolderName(files.get(0).getParentFile()));
		
		if (containsOnly(files, FOLDERS)) {
			loadFolders(files);
		} else if (containsOnly(files, TORRENT_FILES)) {
			loadTorrents(files);
		} else {
			loadFiles(files);
		}
	}
	

	private void loadFolders(List<File> folders) {
		if (folders.size() == 1) {
			// if only one folder was dropped, use its name as title
			list.setTitle(FileUtilities.getFolderName(folders.get(0)));
		}
		
		for (File folder : folders) {
			loadFiles(Arrays.asList(folder.listFiles()));
		}
	}
	

	private void loadTorrents(List<File> files) throws IOException {
		List<Torrent> torrents = new ArrayList<Torrent>(files.size());
		
		for (File file : files) {
			torrents.add(new Torrent(file));
		}
		
		if (torrents.size() == 1) {
			list.setTitle(FileUtilities.getNameWithoutExtension(torrents.get(0).getName()));
		}
		
		for (Torrent torrent : torrents) {
			for (Torrent.Entry entry : torrent.getFiles()) {
				list.getModel().add(FileUtilities.getNameWithoutExtension(entry.getName()));
			}
		}
	}
	

	private void loadFiles(List<File> files) {
		for (File file : files) {
			list.getModel().add(FileUtilities.getName(file));
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files, folders and torrents";
	}
	
}
