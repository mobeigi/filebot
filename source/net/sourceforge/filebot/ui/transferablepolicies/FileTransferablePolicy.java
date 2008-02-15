
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.ui.FileBotUtil;


public abstract class FileTransferablePolicy extends TransferablePolicy {
	
	@Override
	public boolean accept(Transferable tr) {
		if (!isEnabled())
			return false;
		
		List<File> files = getFilesFromTransferable(tr);
		
		if ((files == null) || files.isEmpty())
			return false;
		
		return accept(files);
	}
	

	@SuppressWarnings("unchecked")
	protected List<File> getFilesFromTransferable(Transferable tr) {
		try {
			if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
			} else if (tr.isDataFlavorSupported(FileBotUtil.uriListFlavor)) {
				String transferString = (String) tr.getTransferData(FileBotUtil.uriListFlavor);
				
				String lines[] = transferString.split("\r?\n");
				ArrayList<File> files = new ArrayList<File>(lines.length);
				
				for (String line : lines) {
					if (line.startsWith("#")) {
						// the line is a comment (as per the RFC 2483)
						continue;
					}
					
					try {
						File file = new File(new URI(line));
						
						if (file.exists())
							files.add(file);
					} catch (Exception e) {
						// URISyntaxException, IllegalArgumentException 
						Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid file url: " + line);
					}
				}
				
				return files;
			}
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	

	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files == null)
			return;
		
		Collections.sort(files);
		
		if (!add)
			clear();
		
		load(files);
	}
	

	protected boolean accept(List<File> files) {
		for (File f : files)
			if (!accept(f))
				return false;
		
		return true;
	}
	

	protected void load(List<File> files) {
		for (File file : files) {
			load(file);
		}
	}
	

	protected abstract boolean accept(File file);
	

	protected abstract void clear();
	

	protected abstract boolean load(File file);
	
}
