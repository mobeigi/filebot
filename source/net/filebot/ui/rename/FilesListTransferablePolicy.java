package net.filebot.ui.rename;

import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

import net.filebot.media.MediaDetection;
import net.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.FastFile;
import net.filebot.util.FileUtilities.ExtensionFileFilter;

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
		if (action == TransferAction.LINK || action == TransferAction.PUT) {
			clear();
		}

		super.handleTransferable(tr, action);
	}

	@Override
	protected void load(List<File> files, TransferAction action) {
		load(files, action != TransferAction.LINK);
	}

	protected void load(List<File> files, boolean recursive) {
		Set<File> entries = new LinkedHashSet<File>();
		LinkedList<File> queue = new LinkedList<File>(files);

		while (queue.size() > 0) {
			File f = queue.removeFirst();

			if (f.isHidden())
				continue;

			if (recursive && LIST_FILES.accept(f)) {
				// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
				try (Scanner scanner = new Scanner(createTextReader(f))) {
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

					if (paths.isEmpty()) {
						entries.add(f); // treat as simple text file
					} else {
						queue.addAll(0, paths); // add paths from text file
					}
				} catch (Exception e) {
					debug.log(Level.WARNING, e.getMessage(), e);
				}
			} else if (!recursive || f.isFile() || MediaDetection.isDiskFolder(f)) {
				entries.add(f);
			} else if (f.isDirectory()) {
				queue.addAll(0, sortByUniquePath(getChildren(f))); // FORCE NATURAL FILE ORDER
			}
		}

		publish(FastFile.create(entries));
	}

	@Override
	public String getFileFilterDescription() {
		return "Files and Folders";
	}

	@Override
	public List<String> getFileFilterExtensions() {
		return ExtensionFileFilter.WILDCARD;
	}

	@Override
	protected void process(List<File> chunks) {
		model.addAll(chunks);
	}

	@Override
	protected void process(Exception e) {
		log.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}

}
