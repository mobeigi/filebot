
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileTransferable implements Transferable {
	
	public static final DataFlavor uriListFlavor = createUriListFlavor();
	
	
	private static DataFlavor createUriListFlavor() {
		try {
			return new DataFlavor("text/uri-list;class=java.lang.String");
		} catch (ClassNotFoundException e) {
			// will never happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
		}
		
		return null;
	}
	
	private List<File> files;
	
	private DataFlavor[] supportedFlavors = { DataFlavor.javaFileListFlavor, uriListFlavor };
	
	
	public FileTransferable(File... fileArray) {
		files = new ArrayList<File>(fileArray.length);
		
		for (File file : fileArray)
			files.add(file);
	}
	

	public FileTransferable(Collection<File> fileCollection) {
		files = new ArrayList<File>(fileCollection);
	}
	

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.isFlavorJavaFileListType())
			return files;
		else if (flavor.equals(uriListFlavor))
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
			sb.append("file://" + file.toURI().getPath());
			sb.append("\r\n");
		}
		
		return sb.toString();
	}
	

	public DataFlavor[] getTransferDataFlavors() {
		return supportedFlavors;
	}
	

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor supportedFlavor : supportedFlavors) {
			if (flavor.equals(supportedFlavor))
				return true;
		}
		
		return false;
	}
	
}
