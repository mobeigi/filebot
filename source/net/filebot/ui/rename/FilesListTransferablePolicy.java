package net.filebot.ui.rename;

import static net.filebot.MediaTypes.*;
import static net.filebot.ui.NotificationLogging.*;
import static net.filebot.ui.transfer.FileTransferable.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.filebot.media.MediaDetection;
import net.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.FastFile;

class FilesListTransferablePolicy extends BackgroundFileTransferablePolicy<File> {

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
	public void handleTransferable(Transferable tr, TransferAction action) throws Exception {
		if (action == TransferAction.LINK) {
			// special handling for do-not-resolve-folders-drop
			clear();
			load(getFilesFromTransferable(tr), false);
		} else {
			// load files recursively by default
			super.handleTransferable(tr, action);
		}
	}

	@Override
	protected void load(List<File> files) {
		load(files, true);
	}

	protected void load(List<File> files, boolean recursive) {
		List<File> entries = new ArrayList<File>();
		LinkedList<File> queue = new LinkedList<File>(files);

		while (queue.size() > 0) {
			File f = queue.removeFirst();

			if (f.isHidden())
				continue;

			if (recursive && LIST_FILES.accept(f)) {
				// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
				try {
					Scanner scanner = new Scanner(createTextReader(f));
					List<File> paths = new ArrayList<File>();
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine().trim();
						if (line.length() > 0) {
							File path = new File(line);
							if (path.isAbsolute() && path.exists()) {
								paths.add(path);
							}
						}
					}
					scanner.close();

					if (paths.isEmpty()) {
						entries.add(f); // treat as simple text file
					} else {
						queue.addAll(0, paths); // add paths from text file
					}
				} catch (Exception e) {
					Logger.getLogger(FilesListTransferablePolicy.class.getName()).log(Level.WARNING, e.getMessage());
				}
			} else if (!recursive || f.isFile() || MediaDetection.isDiskFolder(f)) {
				entries.add(f);
			} else if (f.isDirectory()) {
				queue.addAll(0, sortByUniquePath(getChildren(f))); // FORCE NATURAL FILE ORDER
			}
		}

		publish(FastFile.create(entries).toArray(new File[0]));
	}

	@Override
	public String getFileFilterDescription() {
		return "Files and Folders";
	}

	@Override
	protected void process(List<File> chunks) {
		model.addAll(FastFile.create(chunks));
	}

	@Override
	protected void process(Exception e) {
		UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}

}
