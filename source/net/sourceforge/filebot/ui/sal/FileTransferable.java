
package net.sourceforge.filebot.ui.sal;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FileTransferable implements Transferable {
	
	private List<File> files;
	
	
	public FileTransferable(File... fileArray) {
		files = new ArrayList<File>(fileArray.length);
		
		for (File file : fileArray)
			files.add(file);
	}
	

	public FileTransferable(List<File> files) {
		this.files = files;
	}
	

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (!isDataFlavorSupported(flavor))
			throw new UnsupportedFlavorException(flavor);
		
		return files;
	}
	

	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] flavours = { DataFlavor.javaFileListFlavor };
		return flavours;
	}
	

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.isFlavorJavaFileListType();
	}
	
}
