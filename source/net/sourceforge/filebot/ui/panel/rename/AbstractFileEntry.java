
package net.sourceforge.filebot.ui.panel.rename;


class AbstractFileEntry {
	
	private final String name;
	private final long length;
	
	
	public AbstractFileEntry(String name, long length) {
		this.name = name;
		this.length = length;
	}
	

	public String getName() {
		return name;
	}
	

	public long getLength() {
		return length;
	}
	

	@Override
	public String toString() {
		return getName();
	}
	
}
