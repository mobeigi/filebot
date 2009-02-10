
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import javax.swing.SwingWorker;


class ChecksumComputationTask extends SwingWorker<Map<HashType, String>, Void> {
	
	private static final int BUFFER_SIZE = 32 * 1024;
	
	private final File file;
	private final HashType type;
	
	
	public ChecksumComputationTask(File file, HashType type) {
		this.file = file;
		this.type = type;
	}
	

	@Override
	protected Map<HashType, String> doInBackground() throws Exception {
		Hash hash = type.newInstance();
		long length = file.length();
		
		if (length > 0) {
			InputStream in = new FileInputStream(file);
			
			try {
				byte[] buffer = new byte[BUFFER_SIZE];
				
				long position = 0;
				int len = 0;
				
				while ((len = in.read(buffer)) >= 0) {
					position += len;
					
					hash.update(buffer, 0, len);
					
					// update progress
					setProgress((int) ((position * 100) / length));
					
					// check abort status
					if (isCancelled()) {
						break;
					}
				}
			} finally {
				in.close();
			}
		}
		
		return Collections.singletonMap(type, hash.digest());
	}
	
}
