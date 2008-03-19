
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
	

	public static String formatName(File f) {
		String name = f.getName();
		
		if (f.isDirectory())
			return name;
		
		return getNameWithoutExtension(name);
	}
	

	public static String formatNumberOfFiles(int n) {
		if (n == 1)
			return n + " file";
		else
			return n + " files";
	}
	

	public static String getExtension(File f) {
		return getExtension(f, false);
	}
	

	public static String getExtension(File f, boolean includeDot) {
		String name = f.getName();
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
		String name = file.getName();
		
		if (!name.isEmpty())
			return name;
		
		// file might be a drive (only has a path, but no name)
		return file.toString();
	}
	
}
