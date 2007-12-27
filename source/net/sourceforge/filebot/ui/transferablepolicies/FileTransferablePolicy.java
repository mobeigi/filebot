
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class FileTransferablePolicy extends TransferablePolicy {
	
	public boolean accept(Transferable tr) {
		if (!isEnabled())
			return false;
		
		List<File> files = getFilesFromTransferable(tr);
		
		if (files == null || files.isEmpty())
			return false;
		
		return accept(files);
	}
	

	protected List<File> getFilesFromTransferable(Transferable tr) {
		List<File> files = getFilesFromFileTransferable(tr);
		
		// if there is no file transferable, look if there is a string transferable that contains file uris
		if (files == null)
			files = getFilesFromStringTransferable(tr);
		
		Collections.sort(files);
		
		return files;
	}
	

	protected List<File> getFilesFromFileTransferable(Transferable tr) {
		if (!tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			return null;
		
		try {
			List<?> list = (List<?>) tr.getTransferData(DataFlavor.javaFileListFlavor);
			
			ArrayList<File> files = new ArrayList<File>(list.size());
			
			for (Object object : list)
				files.add((File) object);
			
			if (!files.isEmpty())
				return files;
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	

	protected List<File> getFilesFromStringTransferable(Transferable tr) {
		if (!tr.isDataFlavorSupported(DataFlavor.stringFlavor))
			return null;
		
		try {
			String transferString = (String) tr.getTransferData(DataFlavor.stringFlavor);
			
			String lines[] = transferString.split("\r?\n");
			ArrayList<File> files = new ArrayList<File>(lines.length);
			
			for (String line : lines) {
				try {
					File file = new File(new URI(line));
					
					if (file.exists())
						files.add(file);
				} catch (URISyntaxException e) {
					System.err.println(e);
				}
			}
			
			if (!files.isEmpty())
				return files;
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	

	public boolean handleTransferable(Transferable tr, boolean add) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files == null)
			return false;
		
		if (!add)
			clear();
		
		return load(files);
	}
	

	protected boolean accept(List<File> files) {
		for (File f : files)
			if (!accept(f))
				return false;
		
		return true;
	}
	

	protected boolean load(List<File> files) {
		boolean success = false;
		
		for (File file : files) {
			success |= load(file);
		}
		
		return success;
	}
	

	protected abstract boolean accept(File file);
	

	protected abstract void clear();
	

	protected abstract boolean load(File file);
	
}
