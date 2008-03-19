
package net.sourceforge.filebot.ui.panel.rename;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.StringEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.TorrentEntry;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.MultiTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TextTransferablePolicy;


class NamesListTransferablePolicy extends MultiTransferablePolicy {
	
	private NamesRenameList list;
	
	
	public NamesListTransferablePolicy(NamesRenameList list) {
		this.list = list;
		
		addPolicy(new FilePolicy());
		addPolicy(new TextPolicy());
	}
	

	private void submit(List<ListEntry<?>> entries) {
		List<ListEntry<?>> invalidEntries = new ArrayList<ListEntry<?>>();
		
		for (ListEntry<?> entry : entries) {
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
	protected void clear() {
		list.getModel().clear();
	}
	
	
	private class FilePolicy extends FileTransferablePolicy {
		
		private long MAX_FILESIZE = 10 * FileFormat.MEGA;
		
		
		@Override
		protected boolean accept(File file) {
			return file.isFile() && (file.length() < MAX_FILESIZE);
		}
		

		@Override
		protected void load(File file) {
			try {
				List<ListEntry<?>> entries = new ArrayList<ListEntry<?>>();
				
				if (FileFormat.getExtension(file).equalsIgnoreCase("torrent")) {
					Torrent torrent = new Torrent(file);
					
					for (Torrent.Entry entry : torrent.getFiles()) {
						entries.add(new TorrentEntry(entry));
					}
				} else {
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					
					String line = null;
					
					while ((line = in.readLine()) != null)
						if (line.trim().length() > 0)
							entries.add(new StringEntry(line));
					
					in.close();
				}
				
				if (!entries.isEmpty()) {
					submit(entries);
				}
			} catch (Exception e) {
				// should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
		

		@Override
		public String getDescription() {
			return "text files and torrent files";
		}
		
	};
	

	private class TextPolicy extends TextTransferablePolicy {
		
		@Override
		protected void load(String text) {
			List<ListEntry<?>> entries = new ArrayList<ListEntry<?>>();
			
			String[] lines = text.split("\n");
			
			for (String line : lines) {
				
				if (!line.isEmpty())
					entries.add(new StringEntry(line));
			}
			
			if (!entries.isEmpty()) {
				submit(entries);
			}
		}
		

		@Override
		public String getDescription() {
			return "lines of text";
		}
	};
	
}
