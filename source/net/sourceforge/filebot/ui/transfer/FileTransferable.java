
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		return null;
	}
	
	private final List<File> files;
	
	private final DataFlavor[] supportedFlavors = { DataFlavor.javaFileListFlavor, uriListFlavor };
	
	
	public FileTransferable(File... files) {
		this(Arrays.asList(files));
	}
	

	public FileTransferable(Collection<File> files) {
		this.files = new ArrayList<File>(files);
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
		StringBuilder sb = new StringBuilder();
		
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
