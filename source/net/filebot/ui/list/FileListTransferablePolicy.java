package net.filebot.ui.list;

import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.filebot.MediaTypes;
import net.filebot.torrent.Torrent;
import net.filebot.ui.FileBotList;
import net.filebot.ui.transfer.FileTransferablePolicy;
import net.filebot.util.FileUtilities;

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

		// clear selection
		list.getListComponent().clearSelection();

		if (containsOnly(files, MediaTypes.getDefaultFilter("application/torrent"))) {
			loadTorrents(files);
		} else {
			// if only one folder was dropped, use its name as title
			if (files.size() == 1 && files.get(0).isDirectory()) {
				list.setTitle(FileUtilities.getFolderName(files.get(0)));
			}

			// load all files from the given folders recursively up do a depth of 32
			for (File file : listFiles(files)) {
				list.getModel().add(FileUtilities.getName(file));
			}
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

	@Override
	public String getFileFilterDescription() {
		return "files, folders and torrents";
	}

}
