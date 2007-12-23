
package net.sourceforge.filebot.ui;


import java.io.File;
import java.text.NumberFormat;


public class FileFormat {
	
	private static NumberFormat nf = NumberFormat.getNumberInstance();
	
	public final static long KILO = 1024;
	
	public final static long MEGA = KILO * 1024;
	
	public final static long GIGA = MEGA * 1024;
	
	static {
		nf.setMaximumFractionDigits(0);
	}
	
	
	public static String formatSize(long size) {
		if (size >= MEGA)
			return nf.format((double) size / MEGA) + " MB";
		else if (size >= KILO)
			return nf.format((double) size / KILO) + " KB";
		else
			return nf.format((double) size) + " Byte";
	}
	

	public static String formatName(File f) {
		String name = f.getName();
		
		if (f.isDirectory())
			return name;
		
		return getNameWithoutSuffix(name);
	}
	

	public static String formatNumberOfFiles(int n) {
		if (n == 1)
			return n + " file";
		else
			return n + " files";
	}
	

	public static String getSuffix(File f) {
		return getSuffix(f, false);
	}
	

	public static String getSuffix(File f, boolean dot) {
		String name = f.getName();
		int dotIndex = name.lastIndexOf(".");
		
		if (dotIndex > 1) {
			String suffix = name.substring(dotIndex + 1, name.length());
			if (dot)
				return "." + suffix;
			else
				return suffix;
		} else
			return "";
	}
	

	public static String getNameWithoutSuffix(String name) {
		int dotIndex = name.lastIndexOf(".");
		
		if (dotIndex < 1)
			return name;
		
		return name.substring(0, dotIndex);
	}
	

	public static String getNameWithoutSuffix(File file) {
		return getNameWithoutSuffix(file.getName());
	}
	

	public static String getName(File file) {
		if (!file.getName().isEmpty()) {
			return file.getName();
		} else {
			return file.toString();
		}
	}
	
}
