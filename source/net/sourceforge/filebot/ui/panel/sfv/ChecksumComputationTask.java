
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.SwingWorker;


class ChecksumComputationTask extends SwingWorker<Map<HashType, String>, Void> {
	
	private static final int BUFFER_SIZE = 32 * 1024;
	
	private final File file;
	
	
	public ChecksumComputationTask(File file) {
		this.file = file;
	}
	

	@Override
	protected Map<HashType, String> doInBackground() throws Exception {
		Map<HashType, Hash> hashes = new EnumMap<HashType, Hash>(HashType.class);
		
		for (HashType type : HashType.values()) {
			hashes.put(type, type.newInstance());
		}
		
		long length = file.length();
		
		if (length > 0) {
			InputStream in = new FileInputStream(file);
			
			try {
				byte[] buffer = new byte[BUFFER_SIZE];

				long position = 0;
				int len = 0;
				
				while ((len = in.read(buffer)) >= 0) {
					position += len;
					
					for (Hash hash : hashes.values()) {
						hash.update(buffer, 0, len);
					}
					
					// update progress
					setProgress((int) ((position * 100) / length));
					
					// check abort status
					if (isCancelled() || Thread.interrupted()) {
						break;
					}
				}
			} finally {
				in.close();
			}
		}
		
		return digest(hashes);
	}
	

	private Map<HashType, String> digest(Map<HashType, Hash> hashes) {
		Map<HashType, String> results = new EnumMap<HashType, String>(HashType.class);
		
		for (Entry<HashType, Hash> entry : hashes.entrySet()) {
			results.put(entry.getKey(), entry.getValue().digest());
		}
		
		return results;
	}
	
}
