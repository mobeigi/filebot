
package net.sourceforge.tuned;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class TemporaryFolder {
	
	private static final String tmpdir = System.getProperty("java.io.tmpdir");
	
	private static final Map<String, TemporaryFolder> folders = new HashMap<String, TemporaryFolder>();
	
	
	public static TemporaryFolder getFolder(String name) {
		synchronized (folders) {
			TemporaryFolder folder = folders.get(name);
			
			if (folder == null) {
				folder = new TemporaryFolder(new File(tmpdir, name));
				folders.put(name, folder);
			}
			
			return folder;
		}
	}
	
	/**
	 * Delete all temporary folders on shutdown
	 */
	static {
		Runtime.getRuntime().addShutdownHook(new Thread("TemporaryFolder Cleanup") {
			
			@Override
			public void run() {
				synchronized (folders) {
					for (TemporaryFolder folder : folders.values()) {
						folder.delete();
					}
					
					folders.clear();
				}
			}
		});
	}
	
	private final File root;
	
	
	private TemporaryFolder(File root) {
		this.root = root;
	}
	

	public File createFile(String name) throws IOException {
		if (!root.exists())
			root.mkdir();
		
		File file = new File(root, name);
		file.createNewFile();
		
		return file;
	}
	

	public void deleteFile(String name) {
		File file = new File(root, name);
		
		if (file.exists()) {
			file.delete();
		}
	}
	

	public void delete() {
		delete(root);
	}
	

	/**
	 * Delete files/folders recursively
	 * 
	 * @param file file/folder that will be deleted
	 */
	private void delete(File file) {
		if (file.isDirectory()) {
			for (File entry : file.listFiles()) {
				delete(entry);
			}
		}
		
		file.delete();
	}
	
}
