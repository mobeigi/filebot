
package net.sourceforge.filebot.ui.transfer;


import static net.sourceforge.filebot.ui.transfer.FileTransferable.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;


public abstract class FileTransferablePolicy extends TransferablePolicy {
	
	@Override
	public boolean accept(Transferable tr) throws Exception {
		try {
			List<File> files = getFilesFromTransferable(tr);
			
			if (!containsOnly(files, TEMPORARY)) {
				return accept(getFilesFromTransferable(tr));
			}
		} catch (UnsupportedFlavorException e) {
			// no file list flavor
		}
		
		return false;
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
