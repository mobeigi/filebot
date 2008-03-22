
package net.sourceforge.filebot;


import java.io.File;
import java.text.NumberFormat;


public class FileFormat {
	
	private static final NumberFormat numberFormat = NumberFormat.getNumberInstance();
	
	public final static long KILO = 1024;
	
	public final static long MEGA = KILO * 1024;
	
	public final static long GIGA = MEGA * 1024;
	
	static {
		numberFormat.setMaximumFractionDigits(0);
	}
	
	
	public static String formatSize(long size) {
		if (size >= MEGA)
			return numberFormat.format((double) size / MEGA) + " MB";
		else if (size >= KILO)
			return numberFormat.format((double) size / KILO) + " KB";
		else
			return numberFormat.format(size) + " Byte";
	}
	

	public static String formatName(File file) {
		String name = file.getName();
		
		if (file.isDirectory())
			return name;
		
		return getNameWithoutExtension(name);
	}
	

	public static String formatNumberOfFiles(int n) {
		if (n == 1)
			return n + " file";
		else
			return n + " files";
	}
	

	public static boolean hasExtension(File file, String... extensions) {
		if (!file.isFile())
			return false;
		
		String extension = getExtension(file);
		
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
		String name = file.getName();
		int dotIndex = name.lastIndexOf(".");
		
		// .config -> no extension
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
	

	public static String getNameWithoutExtension(File file) {
		return getNameWithoutExtension(file.getName());
	}
	

	public static String getName(File file) {
		if (file == null)
			return "";
		
		String name = file.getName();
		
		if (!name.isEmpty())
			return name;
		
		// file might be a drive (only has a path, but no name)
		return file.toString();
	}
	
}
