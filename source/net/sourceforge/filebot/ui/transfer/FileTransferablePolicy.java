
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public abstract class FileTransferablePolicy extends TransferablePolicy {
	
	/**
	 * Pattern that will match Windows (\r\n), Unix (\n) and Mac (\r) line separators.
	 */
	public static final Pattern LINE_SEPARATOR = Pattern.compile("\r\n|[\r\n]");
	
	
	@Override
	public boolean accept(Transferable tr) throws Exception {
		List<File> files = getFilesFromTransferable(tr);
		
		if (files.isEmpty())
			return false;
		
		return accept(files);
	}
	

	@SuppressWarnings("unchecked")
	protected List<File> getFilesFromTransferable(Transferable tr) throws Exception {
		if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			// file list flavor
			return (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
		} else if (tr.isDataFlavorSupported(FileTransferable.uriListFlavor)) {
			// file URI list flavor
			String transferData = (String) tr.getTransferData(FileTransferable.uriListFlavor);
			
			Scanner scanner = new Scanner(transferData).useDelimiter(LINE_SEPARATOR);
			
			List<File> files = new ArrayList<File>();
			
			while (scanner.hasNext()) {
				String uri = scanner.next();
				
				if (uri.startsWith("#")) {
					// the line is a comment (as per RFC 2483)
					continue;
				}
				
				try {
					File file = new File(new URI(uri));
					
					if (!file.exists())
						throw new FileNotFoundException(file.toString());
					
					files.add(file);
				} catch (Exception e) {
					// URISyntaxException, IllegalArgumentException, FileNotFoundException
					Logger.getLogger("global").log(Level.WARNING, "Invalid file uri: " + uri);
				}
			}
			
			return files;
		}
		
		return Collections.emptyList();
	}
	

	@Override
	public void handleTransferable(Transferable tr, TransferAction action) throws Exception {
		List<File> files = getFilesFromTransferable(tr);
		
		if (action == TransferAction.PUT) {
			clear();
		}
		
		load(files);
	}
	

	protected abstract boolean accept(List<File> files);
	

	protected abstract void load(List<File> files) throws IOException;
	

	protected abstract void clear();
	

	public String getFileFilterDescription() {
		return null;
	}
	
}
