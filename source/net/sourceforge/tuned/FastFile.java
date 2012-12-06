
package net.sourceforge.tuned;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
		if (names == null)
			return null;
		
		File[] files = new File[names.length];
		for (int i = 0; i < names.length; i++) {
			files[i] = new FastFile(this, names[i]);
		}
		return files;
	}
	
	
	public static List<FastFile> foreach(File... files) {
		return foreach(Arrays.asList(files));
	}
	
	
	public static List<FastFile> foreach(final List<File> files) {
		List<FastFile> result = new ArrayList<FastFile>(files.size());
		
		for (File file : files) {
			result.add(new FastFile(file.getPath()));
		}
		
		return result;
	}
}
