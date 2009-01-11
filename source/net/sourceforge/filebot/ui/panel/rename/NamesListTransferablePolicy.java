
package net.sourceforge.filebot.ui.panel.rename;


import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static net.sourceforge.filebot.FileBotUtil.LIST_FILE_EXTENSIONS;
import static net.sourceforge.filebot.FileBotUtil.TORRENT_FILE_EXTENSIONS;
import static net.sourceforge.filebot.FileBotUtil.containsOnly;
import static net.sourceforge.filebot.FileBotUtil.isInvalidFileName;
import static net.sourceforge.tuned.FileUtil.getNameWithoutExtension;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sourceforge.filebot.torrent.Torrent;


class NamesListTransferablePolicy extends FilesListTransferablePolicy {
	
	private final RenameList<Object> list;
	
	
	public NamesListTransferablePolicy(RenameList<Object> list) {
		super(list.getModel());
		
		this.list = list;
	}
	

	@Override
	public boolean accept(Transferable tr) {
		return tr.isDataFlavorSupported(stringFlavor) || super.accept(tr);
	}
	

	@Override
	public void handleTransferable(Transferable tr, TransferAction action) {
		if (action == TransferAction.PUT) {
			clear();
		}
		
		if (tr.isDataFlavorSupported(stringFlavor)) {
			// string transferable
			try {
				load((String) tr.getTransferData(stringFlavor));
			} catch (UnsupportedFlavorException e) {
				// should not happen
				throw new RuntimeException(e);
			} catch (IOException e) {
				// should not happen
				throw new RuntimeException(e);
			}
		} else if (super.accept(tr)) {
			// file transferable
			load(getFilesFromTransferable(tr));
		}
	}
	

	protected void submit(List<StringEntry> entries) {
		List<StringEntry> invalidEntries = new ArrayList<StringEntry>();
		
		for (StringEntry entry : entries) {
			if (isInvalidFileName(entry.getValue()))
				invalidEntries.add(entry);
		}
		
		if (!invalidEntries.isEmpty()) {
			ValidateNamesDialog dialog = new ValidateNamesDialog(SwingUtilities.getWindowAncestor(list), invalidEntries);
			dialog.setVisible(true);
			
			if (dialog.isCancelled()) {
				// return immediately, don't add items to list
				return;
			}
		}
		
		list.getModel().addAll(entries);
	}
	

	protected void load(String string) {
		List<StringEntry> entries = new ArrayList<StringEntry>();
		
		Scanner scanner = new Scanner(string).useDelimiter(LINE_SEPARATOR);
		
		while (scanner.hasNext()) {
			String line = scanner.next();
			
			if (line.trim().length() > 0) {
				entries.add(new StringEntry(line));
			}
		}
		
		submit(entries);
	}
	

	@Override
	protected void load(List<File> files) {
		if (containsOnly(files, LIST_FILE_EXTENSIONS)) {
			loadListFiles(files);
		} else if (containsOnly(files, TORRENT_FILE_EXTENSIONS)) {
			loadTorrentFiles(files);
		} else {
			super.load(files);
		}
	}
	

	private void loadListFiles(List<File> files) {
		try {
			List<StringEntry> entries = new ArrayList<StringEntry>();
			
			for (File file : files) {
				Scanner scanner = new Scanner(file, "UTF-8").useDelimiter(LINE_SEPARATOR);
				
				while (scanner.hasNext()) {
					String line = scanner.next();
					
					if (line.trim().length() > 0) {
						entries.add(new StringEntry(line));
					}
				}
				
				scanner.close();
			}
			
			submit(entries);
		} catch (IOException e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	private void loadTorrentFiles(List<File> files) {
		try {
			List<AbstractFileEntry> entries = new ArrayList<AbstractFileEntry>();
			
			for (File file : files) {
				Torrent torrent = new Torrent(file);
				
				for (Torrent.Entry entry : torrent.getFiles()) {
					entries.add(new AbstractFileEntry(getNameWithoutExtension(entry.getName()), entry.getLength()));
				}
			}
			
			// add torrent entries directly without checking file names for invalid characters
			list.getModel().addAll(entries);
		} catch (IOException e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "text files and torrent files";
	}
	
}
