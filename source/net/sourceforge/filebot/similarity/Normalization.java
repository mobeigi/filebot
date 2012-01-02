
package net.sourceforge.filebot.similarity;


public class Normalization {
	
	public static String normalizePunctuation(String name) {
		// remove/normalize special characters
		name = name.replaceAll("['`´]+", "");
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		return name.trim();
	}
	
	
	public static String normalizeBrackets(String name) {
		// remove group names and checksums, any [...] or (...)
		name = name.replaceAll("\\([^\\(]*\\)", " ");
		name = name.replaceAll("\\[[^\\[]*\\]", " ");
		name = name.replaceAll("\\{[^\\{]*\\}", " ");
		
		return name;
	}
	
	
	public static String removeEmbeddedChecksum(String string) {
		// match embedded checksum and surrounding brackets 
		return string.replaceAll("[\\(\\[]\\p{XDigit}{8}[\\]\\)]", "");
	}
	
}
