package net.filebot.ui.rename;

import static java.nio.charset.StandardCharsets.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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
		Set<File> fileset = new LinkedHashSet<File>();

		// load files recursively by default
		load(files, action != TransferAction.LINK, fileset);

		// use fast file to minimize system calls like length(), isDirectory(), isFile(), ...
		publish(fileset.stream().map(FastFile::new).toArray(File[]::new));
	}

	private void load(List<File> files, boolean recursive, Collection<File> sink) {
		for (File f : files) {
			// ignore hidden files
			if (f.isHidden()) {
				continue;
			}

			// load file paths from text files
			if (recursive && LIST_FILES.accept(f)) {
				try {
					List<File> list = Files.lines(f.toPath(), UTF_8).map(File::new).filter(it -> {
						return it.isAbsolute() && it.exists();
					}).collect(toList());

					if (list.isEmpty()) {
						sink.add(f); // treat as simple text file
					} else {
						load(list, false, sink); // add paths from text file
					}
				} catch (Exception e) {
					debug.log(Level.WARNING, e.getMessage(), e);
				}
			}

			// load normal files
			else if (!recursive || f.isFile() || MediaDetection.isDiskFolder(f)) {
				sink.add(f);
			}

			// load folders recursively
			else if (f.isDirectory()) {
				load(sortByUniquePath(getChildren(f)), true, sink); // FORCE NATURAL FILE ORDER
			}
		}
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
