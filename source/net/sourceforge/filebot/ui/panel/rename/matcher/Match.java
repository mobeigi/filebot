
package net.sourceforge.filebot.ui.panel.rename.matcher;


import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class Match {
	
	private final ListEntry a;
	private final ListEntry b;
	
	
	public Match(ListEntry a, ListEntry b) {
		this.a = a;
		this.b = b;
	}
	

	public ListEntry getA() {
		return a;
	}
	

	public ListEntry getB() {
		return b;
	}
	
}
