
package net.sourceforge.filebot.ui.transfer;


import java.io.File;

import javax.swing.filechooser.FileFilter;

import net.sourceforge.filebot.ui.transferablepolicies.FileTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.MultiTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;


public class TransferablePolicyFileFilter extends FileFilter {
	
	private final TransferablePolicy transferablePolicy;
	
	
	public TransferablePolicyFileFilter(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		
		return transferablePolicy.accept(new FileTransferable(f));
	}
	

	@Override
	public String getDescription() {
		if (transferablePolicy instanceof MultiTransferablePolicy) {
			MultiTransferablePolicy multi = (MultiTransferablePolicy) transferablePolicy;
			return multi.getDescription(FileTransferablePolicy.class);
		}
		
		return transferablePolicy.getDescription();
	}
	
}
