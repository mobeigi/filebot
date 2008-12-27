
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.StringEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.TorrentEntry;
import net.sourceforge.filebot.ui.transfer.StringTransferablePolicy;


class NamesListTransferablePolicy extends FilesListTransferablePolicy {
	
	private final RenameList<ListEntry> list;
	
	private final TextPolicy textPolicy = new TextPolicy();
	
	
	public NamesListTransferablePolicy(RenameList<ListEntry> list) {
		super(list.getModel());
		
		this.list = list;
	}
	

	@Override
	public boolean accept(Transferable tr) {
		return textPolicy.accept(tr) || super.accept(tr);
	}
	

	@Override
	public void handleTransferable(Transferable tr, TransferAction action) {
		if (super.accept(tr))
			super.handleTransferable(tr, action);
		else if (textPolicy.accept(tr))
			textPolicy.handleTransferable(tr, action);
	}
	

	private void submit(List<ListEntry> entries) {
		List<ListEntry> invalidEntries = new ArrayList<ListEntry>();
		
		for (ListEntry entry : entries) {
			if (FileBotUtil.isInvalidFileName(entry.getName()))
				invalidEntries.add(entry);
		}
		
		if (!invalidEntries.isEmpty()) {
			ValidateNamesDialog dialog = new ValidateNamesDialog(SwingUtilities.getWindowAncestor(list), invalidEntries);
			dialog.setVisible(true);
			
			if (dialog.isCancelled())
				return;
		}
		
		list.getModel().addAll(entries);
	}
	

	@Override
	protected void load(List<File> files) {
		
		if (FileBotUtil.containsOnlyListFiles(files)) {
			loadListFiles(files);
		} else if (FileBotUtil.containsOnlyTorrentFiles(files)) {
			loadTorrentFiles(files);
		} else {
			super.load(files);
		}
	}
	

	private void loadListFiles(List<File> files) {
		try {
			List<ListEntry> entries = new ArrayList<ListEntry>();
			
			for (File file : files) {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				
				String line = null;
				
				while ((line = in.readLine()) != null) {
					if (line.trim().length() > 0) {
						entries.add(new StringEntry(line));
					}
				}
				
				in.close();
			}
			
			submit(entries);
		} catch (IOException e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	private void loadTorrentFiles(List<File> files) {
		try {
			List<ListEntry> entries = new ArrayList<ListEntry>();
			
			for (File file : files) {
				Torrent torrent = new Torrent(file);
				
				for (Torrent.Entry entry : torrent.getFiles()) {
					entries.add(new TorrentEntry(entry));
				}
			}
			
			submit(entries);
		} catch (IOException e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "text files and torrent files";
	}
	
	
	private class TextPolicy extends StringTransferablePolicy {
		
		@Override
		protected void clear() {
			NamesListTransferablePolicy.this.clear();
		}
		

		@Override
		protected void load(String string) {
			List<ListEntry> entries = new ArrayList<ListEntry>();
			
			String[] lines = string.split("\r?\n");
			
			for (String line : lines) {
				
				if (!line.isEmpty())
					entries.add(new StringEntry(line));
			}
			
			if (!entries.isEmpty()) {
				submit(entries);
			}
		}
		
	}
	
}
