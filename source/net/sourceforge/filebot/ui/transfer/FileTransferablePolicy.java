
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class FileTransferablePolicy extends TransferablePolicy {
	
	@Override
	public boolean accept(Transferable tr) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files.isEmpty())
			return false;
		
		return accept(files);
	}
	

	@SuppressWarnings("unchecked")
	protected List<File> getFilesFromTransferable(Transferable tr) {
		try {
			if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				// file list flavor
				return (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
			} else if (tr.isDataFlavorSupported(FileTransferable.uriListFlavor)) {
				// file uri list flavor
				String transferString = (String) tr.getTransferData(FileTransferable.uriListFlavor);
				
				String lines[] = transferString.split("\r?\n");
				ArrayList<File> files = new ArrayList<File>(lines.length);
				
				for (String line : lines) {
					if (line.startsWith("#")) {
						// the line is a comment (as per the RFC 2483)
						continue;
					}
					
					try {
						File file = new File(new URI(line));
						
						if (!file.exists())
							throw new FileNotFoundException(file.toString());
						
						files.add(file);
					} catch (Exception e) {
						// URISyntaxException, IllegalArgumentException, FileNotFoundException
						Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid file url: " + line);
					}
				}
				
				return files;
			}
		} catch (UnsupportedFlavorException e) {
			// should not happen
			throw new RuntimeException(e);
		} catch (IOException e) {
			// should not happen
			throw new RuntimeException(e);
		}
		
		return Collections.EMPTY_LIST;
	}
	

	@Override
	public void handleTransferable(Transferable tr, TransferAction action) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (action != TransferAction.ADD)
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
	

	protected boolean accept(File file) {
		return file.isFile() || file.isDirectory();
	}
	

	protected void clear() {
		
	}
	

	protected void load(File file) {
		
	}
	

	public String getFileFilterDescription() {
		return null;
	}
	
}
