
package net.sourceforge.tuned;


import java.io.File;


public class FastFile extends File {
	
	private Long length;
	private Boolean isDirectory;
	private Boolean isFile;
	
	
	public FastFile(String path) {
		super(path);
	}
	

	public FastFile(File parent, String child) {
		super(parent, child);
	}
	

	@Override
	public long length() {
		return length != null ? length : (length = super.length());
	}
	

	@Override
	public boolean isDirectory() {
		return isDirectory != null ? isDirectory : (isDirectory = super.isDirectory());
	}
	

	@Override
	public boolean isFile() {
		return isFile != null ? isFile : (isFile = super.isFile());
	}
	

	@Override
	public File[] listFiles() {
		String[] names = list();
		File[] files = new File[names.length];
		
		for (int i = 0; i < names.length; i++) {
			files[i] = new FastFile(this, names[i]);
		}
		
		return files;
	}
	
}
