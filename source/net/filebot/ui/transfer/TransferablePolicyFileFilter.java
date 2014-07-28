package net.filebot.ui.transfer;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class TransferablePolicyFileFilter extends FileFilter implements FilenameFilter {

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
	public boolean accept(File dir, String name) {
		return accept(new File(dir, name));
	}

	@Override
	public String getDescription() {
		return this.toString();
	}

	@Override
	public String toString() {
		if (transferablePolicy instanceof FileTransferablePolicy) {
			return ((FileTransferablePolicy) transferablePolicy).getFileFilterDescription();
		}
		return super.toString();
	}

}
