
package net.sourceforge.filebot.ui.transfer;


import java.io.File;

import javax.swing.filechooser.FileFilter;


public class TransferablePolicyFileFilter extends FileFilter {
	
	private final TransferablePolicy transferablePolicy;
	
	
	public TransferablePolicyFileFilter(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		
		try {
			return transferablePolicy.accept(new FileTransferable(f));
		} catch (Exception e) {
			return false;
		}
	}
	

	@Override
	public String getDescription() {
		if (transferablePolicy instanceof FileTransferablePolicy) {
			return ((FileTransferablePolicy) transferablePolicy).getFileFilterDescription();
		}
		
		return null;
	}
}
