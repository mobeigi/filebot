
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sourceforge.filebot.ui.FileBotUtil;


public class FileTransferable implements Transferable {
	
	private static final boolean fileListFlavorSupported = FileBotUtil.isFileListFlavorSupportedByWindowManager();
	
	private List<File> files;
	
	
	public FileTransferable(File... fileArray) {
		files = new ArrayList<File>(fileArray.length);
		
		for (File file : fileArray)
			files.add(file);
	}
	

	public FileTransferable(Collection<File> fileCollection) {
		files = new ArrayList<File>(fileCollection.size());
		
		files.addAll(fileCollection);
	}
	

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.isFlavorJavaFileListType())
			return files;
		else if (flavor.isFlavorTextType())
			return getUriList();
		else
			throw new UnsupportedFlavorException(flavor);
	}
	

	/**
	 * 
	 * @return line separated list of file uris
	 */
	private String getUriList() {
		StringBuffer sb = new StringBuffer();
		
		for (File file : files) {
			sb.append(file.toURI());
			sb.append("\n");
		}
		
		return sb.toString();
	}
	

	public DataFlavor[] getTransferDataFlavors() {
		if (fileListFlavorSupported) {
			DataFlavor[] flavours = { DataFlavor.javaFileListFlavor };
			return flavours;
		} else {
			DataFlavor[] flavours = { DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor };
			return flavours;
		}
	}
	

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (fileListFlavorSupported)
			return flavor.isFlavorJavaFileListType();
		else
			return flavor.isFlavorJavaFileListType() || flavor.isFlavorTextType();
	}
	
}
