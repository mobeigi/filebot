
package net.sourceforge.filebot.ui.panel.rename;


import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static net.sourceforge.filebot.FileBotUtilities.LIST_FILES;
import static net.sourceforge.filebot.FileBotUtilities.TORRENT_FILES;
import static net.sourceforge.tuned.FileUtilities.FOLDERS;
import static net.sourceforge.tuned.FileUtilities.containsOnly;
import static net.sourceforge.tuned.FileUtilities.getNameWithoutExtension;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import net.sourceforge.tuned.FileUtilities;


class NamesListTransferablePolicy extends FileTransferablePolicy {
	
	private final List<Object> model;
	
	
	public NamesListTransferablePolicy(List<Object> model) {
		this.model = model;
	}
	

	@Override
	protected void clear() {
		model.clear();
	}
	

	@Override
	public boolean accept(Transferable tr) throws Exception {
		return tr.isDataFlavorSupported(stringFlavor) || super.accept(tr);
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	public void handleTransferable(Transferable tr, TransferAction action) throws Exception {
		if (action == TransferAction.PUT) {
			clear();
		}
		
		if (tr.isDataFlavorSupported(stringFlavor)) {
			// string transferable
			load((String) tr.getTransferData(stringFlavor));
		} else if (super.accept(tr)) {
			// file transferable
			load(getFilesFromTransferable(tr));
		}
	}
	

	protected void load(String string) {
		List<MutableString> entries = new ArrayList<MutableString>();
		
		Scanner scanner = new Scanner(string).useDelimiter(LINE_SEPARATOR);
		
		while (scanner.hasNext()) {
			String line = scanner.next();
			
			if (line.trim().length() > 0) {
				entries.add(new MutableString(line));
			}
		}
		
		model.addAll(entries);
	}
	

	@Override
	protected void load(List<File> files) throws FileNotFoundException {
		if (containsOnly(files, LIST_FILES)) {
			loadListFiles(files);
		} else if (containsOnly(files, TORRENT_FILES)) {
			loadTorrentFiles(files);
		} else if (containsOnly(files, FOLDERS)) {
			// load files from each folder
			for (File folder : files) {
				loadFiles(Arrays.asList(folder.listFiles()));
			}
		} else {
			loadFiles(files);
		}
	}
	

	protected void loadFiles(List<File> files) {
		for (File file : files) {
			model.add(new AbstractFileEntry(FileUtilities.getName(file), file.length()));
		}
	}
	

	protected void loadListFiles(List<File> files) throws FileNotFoundException {
		List<String> values = new ArrayList<String>();
		
		for (File file : files) {
			// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
			Scanner scanner = new Scanner(new FileInputStream(file), "UTF-8").useDelimiter(LINE_SEPARATOR);
			
			while (scanner.hasNext()) {
				String line = scanner.next();
				
				if (line.trim().length() > 0) {
					values.add(line);
				}
			}
			
			scanner.close();
		}
		
		model.addAll(values);
	}
	

	protected void loadTorrentFiles(List<File> files) {
		try {
			List<AbstractFileEntry> entries = new ArrayList<AbstractFileEntry>();
			
			for (File file : files) {
				Torrent torrent = new Torrent(file);
				
				for (Torrent.Entry entry : torrent.getFiles()) {
					entries.add(new AbstractFileEntry(getNameWithoutExtension(entry.getName()), entry.getLength()));
				}
			}
			
			// add torrent entries directly without checking file names for invalid characters
			model.addAll(entries);
		} catch (IOException e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "text files and torrent files";
	}
	
}
