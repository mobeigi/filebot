
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class AbstractFileEntry<T> extends ListEntry<T> {
	
	private final long length;
	
	private final String type;
	
	
	public AbstractFileEntry(String name, T value, String type, long length) {
		super(name, value);
		this.length = length;
		this.type = type;
	}
	

	public long getLength() {
		return length;
	}
	

	public String getType() {
		return type;
	}
	
}
