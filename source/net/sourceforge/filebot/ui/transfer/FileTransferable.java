
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileTransferable implements Transferable {
	
	public static final DataFlavor uriListFlavor = createUriListFlavor();
	
	
	private static DataFlavor createUriListFlavor() {
		try {
			return new DataFlavor("text/uri-list;class=java.lang.String");
		} catch (ClassNotFoundException e) {
			// will never happen
			Logger.getLogger(FileTransferable.class.getName()).log(Level.SEVERE, e.toString(), e);
		}
		
		return null;
	}
	
	private final Collection<File> files;
	
	
	public FileTransferable(File... files) {
		this(Arrays.asList(files));
	}
	

	public FileTransferable(Collection<File> files) {
		this.files = new ArrayList<File>(files);
	}
	

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.isFlavorJavaFileListType())
			return files;
		else if (flavor.equals(uriListFlavor))
			return getUriList();
		else
			throw new UnsupportedFlavorException(flavor);
	}
	

	/**
	 * @return line separated list of file URIs
	 */
	private String getUriList() {
		StringBuilder sb = new StringBuilder(80 * files.size());
		
		for (File file : files) {
			sb.append("file://" + file.toURI().getPath());
			sb.append("\r\n");
		}
		
		return sb.toString();
	}
	

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.javaFileListFlavor, uriListFlavor };
	}
	

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return isFileListFlavor(flavor);
	}
	

	public static boolean isFileListFlavor(DataFlavor flavor) {
		return flavor.isFlavorJavaFileListType() || flavor.equals(uriListFlavor);
	}
	
}
