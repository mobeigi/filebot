
package net.sourceforge.filebot.ui.panel.rename;


import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static net.sourceforge.filebot.FileBotUtilities.LIST_FILES;
import static net.sourceforge.filebot.FileBotUtilities.TORRENT_FILES;
import static net.sourceforge.tuned.FileUtilities.FOLDERS;
import static net.sourceforge.tuned.FileUtilities.containsOnly;
import static net.sourceforge.tuned.FileUtilities.getNameWithoutExtension;

import java.awt.datatransfer.DataFlavor;
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
import net.sourceforge.filebot.ui.transfer.ArrayTransferable;
import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.tuned.FastFile;


class NamesListTransferablePolicy extends FileTransferablePolicy {
	
	private static final DataFlavor episodeArrayFlavor = ArrayTransferable.flavor(Episode.class);
	
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
		
		if (tr.isDataFlavorSupported(episodeArrayFlavor)) {
			// episode array transferable
			model.addAll(Arrays.asList((Episode[]) tr.getTransferData((episodeArrayFlavor))));
		} else if (super.accept(tr)) {
			// file transferable
			load(getFilesFromTransferable(tr));
		} else if (tr.isDataFlavorSupported(stringFlavor)) {
			// string transferable
			load((String) tr.getTransferData(stringFlavor));
		}
	}
	

	protected void load(String string) {
		List<String> values = new ArrayList<String>();
		
		Scanner scanner = new Scanner(string).useDelimiter(LINE_SEPARATOR);
		
		while (scanner.hasNext()) {
			String line = scanner.next();
			
			if (line.trim().length() > 0) {
				values.add(line);
			}
		}
		
		model.addAll(values);
	}
	

	@Override
	protected void load(List<File> files) throws FileNotFoundException {
		List<Object> values = new ArrayList<Object>();
		
		if (containsOnly(files, LIST_FILES)) {
			loadListFiles(files, values);
		} else if (containsOnly(files, TORRENT_FILES)) {
			loadTorrentFiles(files, values);
		} else if (containsOnly(files, FOLDERS)) {
			// load files from each folder
			for (File folder : files) {
				values.addAll(FastFile.foreach(folder.listFiles()));
			}
		} else {
			values.addAll(FastFile.foreach(files));
		}
		
		model.addAll(values);
	}
	

	protected void loadListFiles(List<File> files, List<Object> values) throws FileNotFoundException {
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
	}
	

	protected void loadTorrentFiles(List<File> files, List<Object> values) {
		try {
			for (File file : files) {
				Torrent torrent = new Torrent(file);
				
				for (Torrent.Entry entry : torrent.getFiles()) {
					values.add(new AbstractFileEntry(getNameWithoutExtension(entry.getName()), entry.getLength()));
				}
			}
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "text files and torrent files";
	}
	
}
