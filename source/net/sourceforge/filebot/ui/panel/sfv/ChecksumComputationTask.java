
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.io.FileInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.swing.SwingWorker;


public class ChecksumComputationTask extends SwingWorker<Long, Object> {
	
	private static final int MAX_READ_LENGTH = 200 * 1024; // 200 KB
	
	private File file;
	
	
	public ChecksumComputationTask(File file) {
		this.file = file;
	}
	

	@Override
	protected Long doInBackground() throws Exception {
		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
		
		long length = file.length();
		long done = 0;
		
		int bufferLength = (int) Math.min(length, MAX_READ_LENGTH);
		
		// don't allow bufferLength == 0
		if (bufferLength < 1)
			bufferLength = 1;
		
		byte[] buffer = new byte[bufferLength];
		
		int bytesRead = 0;
		
		while ((bytesRead = cis.read(buffer)) >= 0) {
			if (isCancelled())
				break;
			
			done += bytesRead;
			
			if (length > 0) {
				int progress = (int) ((done * 100) / length);
				setProgress(progress);
			}
			
		}
		
		cis.close();
		
		return cis.getChecksum().getValue();
	}
	

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + file.getName() + ")";
	}
	
}
