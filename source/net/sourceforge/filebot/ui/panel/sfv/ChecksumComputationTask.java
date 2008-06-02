
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.io.FileInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.swing.SwingWorker;


public class ChecksumComputationTask extends SwingWorker<Long, Void> {
	
	private static final int BUFFER_SIZE = 32 * 1024;
	
	private final File file;
	
	
	public ChecksumComputationTask(File file) {
		this.file = file;
	}
	

	@Override
	protected Long doInBackground() throws Exception {
		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
		
		long length = file.length();
		
		if (length > 0) {
			long done = 0;
			
			byte[] buffer = new byte[BUFFER_SIZE];
			
			int bytesRead = 0;
			
			while ((bytesRead = cis.read(buffer)) >= 0) {
				if (isCancelled() || Thread.currentThread().isInterrupted())
					break;
				
				done += bytesRead;
				
				int progress = (int) ((done * 100) / length);
				setProgress(progress);
			}
		}
		
		cis.close();
		
		return cis.getChecksum().getValue();
	}
	

	@Override
	public String toString() {
		return String.format("%s (%s)", getClass().getSimpleName(), file.getName());
	}
}
