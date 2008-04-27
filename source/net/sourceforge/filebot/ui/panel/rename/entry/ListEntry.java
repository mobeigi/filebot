
package net.sourceforge.filebot.ui.panel.rename.entry;


public class ListEntry {
	
	private String name;
	
	
	public ListEntry(String name) {
		this.name = name;
	}
	

	public String getName() {
		return name;
	}
	

	public void setName(String name) {
		this.name = name;
	}
	

	@Override
	public String toString() {
		return getName();
	}
}
