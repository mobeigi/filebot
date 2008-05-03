
package net.sourceforge.filebot.ui.panel.subtitle;


public enum ArchiveType {
	ZIP,
	RAR,
	UNKNOWN;
	
	public static ArchiveType forName(String name) {
		if (name == null)
			return UNKNOWN;
		
		if (name.equalsIgnoreCase("zip"))
			return ZIP;
		
		if (name.equalsIgnoreCase("rar"))
			return RAR;
		
		return UNKNOWN;
	}
	
}
