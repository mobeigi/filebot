
package net.sourceforge.filebot.ui.panel.rename;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.ui.FileFormat;
import net.sourceforge.filebot.ui.panel.rename.entry.StringEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.TorrentEntry;
import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.MultiTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TextTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;


public class NamesRenameListTransferablePolicy extends MultiTransferablePolicy {
	
	private DefaultListModel listModel;
	
	
	public NamesRenameListTransferablePolicy(DefaultListModel listModel) {
		this.listModel = listModel;
		
		addPolicy(filePolicy);
		addPolicy(textPolicy);
	}
	
	private TransferablePolicy filePolicy = new FileTransferablePolicy() {
		
		private long MAX_FILESIZE = 10 * FileFormat.MEGA;
		
		
		@Override
		protected boolean accept(File file) {
			return file.isFile() && (file.length() < MAX_FILESIZE);
		}
		

		@Override
		protected void clear() {
			listModel.clear();
		}
		

		@Override
		protected void load(File file) {
			try {
				if (FileFormat.getSuffix(file).equalsIgnoreCase("torrent")) {
					Torrent torrent = new Torrent(file);
					
					for (Torrent.Entry entry : torrent.getFiles()) {
						listModel.addElement(new TorrentEntry(entry));
					}
				} else {
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					
					String line = null;
					
					while ((line = in.readLine()) != null)
						if (line.trim().length() > 0)
							listModel.addElement(new StringEntry(line));
					
					in.close();
				}
			} catch (Exception e) {
				// should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.getMessage(), e);
			}
		}
		

		@Override
		public String getDescription() {
			return "text files and torrent files";
		}
		
	};
	
	private TransferablePolicy textPolicy = new TextTransferablePolicy() {
		
		@Override
		protected boolean load(String text) {
			String[] lines = text.split("\n");
			
			for (String line : lines) {
				
				if (!line.isEmpty())
					listModel.addElement(new StringEntry(line));
			}
			
			return true;
		}
		

		@Override
		protected void clear() {
			listModel.clear();
		}
		

		@Override
		public String getDescription() {
			return "Lines of text";
		}
	};
	
}
