
package net.sourceforge.tuned;


import java.io.File;


public final class FileUtil {
	
	public final static long KILO = 1024;
	
	public final static long MEGA = KILO * 1024;
	
	public final static long GIGA = MEGA * 1024;
	
	
	public static String formatSize(long size) {
		if (size >= MEGA)
			return String.format("%,d MB", size / MEGA);
		else if (size >= KILO)
			return String.format("%,d KB", size / KILO);
		else
			return String.format("%,d Byte", size);
	}
	

	public static boolean hasExtension(File file, String... extensions) {
		if (file.isDirectory())
			return false;
		
		return hasExtension(file.getName(), extensions);
	}
	

	public static boolean hasExtension(String filename, String... extensions) {
		String extension = getExtension(filename, false);
		
		for (String ext : extensions) {
			if (ext.equalsIgnoreCase(extension))
				return true;
		}
		
		return false;
	}
	

	public static String getExtension(File file) {
		return getExtension(file, false);
	}
	

	public static String getExtension(File file, boolean includeDot) {
		return getExtension(file.getName(), includeDot);
	}
	

	public static String getExtension(String name, boolean includeDot) {
		int dotIndex = name.lastIndexOf(".");
		
		// .config -> no extension, just hidden
		if (dotIndex >= 1) {
			int startIndex = dotIndex;
			
			if (!includeDot)
				startIndex += 1;
			
			if (startIndex <= name.length()) {
				return name.substring(startIndex, name.length());
			}
		}
		
		return "";
	}
	

	public static String getNameWithoutExtension(String name) {
		int dotIndex = name.lastIndexOf(".");
		
		if (dotIndex < 1)
			return name;
		
		return name.substring(0, dotIndex);
	}
	

	public static String getFileName(File file) {
		if (file.isDirectory())
			return getFolderName(file);
		
		return getNameWithoutExtension(file.getName());
	}
	

	public static String getFolderName(File file) {
		String name = file.getName();
		
		if (!name.isEmpty())
			return name;
		
		// file might be a drive (only has a path, but no name)
		return file.toString();
	}
	

	public static String getFileType(File file) {
		if (file.isDirectory())
			return "Folder";
		
		String extension = getExtension(file.getName(), false);
		
		if (!extension.isEmpty())
			return extension;
		
		// some file with no suffix
		return "File";
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private FileUtil() {
		throw new UnsupportedOperationException();
	}
	
}
