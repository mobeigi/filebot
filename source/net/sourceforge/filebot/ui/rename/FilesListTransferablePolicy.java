package net.sourceforge.filebot.ui.rename;

import static java.util.Arrays.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.ui.transfer.FileTransferable.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

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
				File[] children = f.listFiles();
				if (children != null) {
					queue.addAll(0, new TreeSet<File>(asList(children))); // FORCE NATURAL FILE ORDER
				}
			}
		}

		model.addAll(FastFile.foreach(entries));
	}

	@Override
	public String getFileFilterDescription() {
		return "files and folders";
	}

}
