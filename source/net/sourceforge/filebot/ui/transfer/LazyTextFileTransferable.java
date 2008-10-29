
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.tuned.TemporaryFolder;


public class LazyTextFileTransferable implements Transferable {
	
	private final String text;
	private final String defaultFileName;
	
	private FileTransferable fileTransferable = null;
	
	
	public LazyTextFileTransferable(String text, String defaultFileName) {
		this.text = text;
		this.defaultFileName = defaultFileName;
	}
	

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.isFlavorTextType()) {
			return text;
		} else if (FileTransferable.isFileListFlavor(flavor)) {
			try {
				// create text file for transfer on demand
				if (fileTransferable == null) {
					fileTransferable = createFileTransferable();
				}
				
				return fileTransferable.getTransferData(flavor);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		throw new UnsupportedFlavorException(flavor);
	}
	

	private FileTransferable createFileTransferable() throws IOException {
		// remove invalid characters from file name
		String validFileName = FileBotUtil.validateFileName(defaultFileName);
		
		// create new temporary file in TEMP/APP_NAME [UUID]/dnd
		File temporaryFile = TemporaryFolder.getFolder(FileBotUtil.getApplicationName()).subFolder("dnd").createFile(validFileName);
		
		// write text to file
		FileChannel fileChannel = new FileOutputStream(temporaryFile).getChannel();
		
		try {
			fileChannel.write(Charset.forName("UTF-8").encode(text));
		} finally {
			fileChannel.close();
		}
		
		return new FileTransferable(temporaryFile);
	}
	

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor, FileTransferable.uriListFlavor };
	}
	

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.isFlavorTextType() || FileTransferable.isFileListFlavor(flavor);
	}
	
}
