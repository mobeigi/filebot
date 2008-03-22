
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class AbstractFileEntry<T> extends ListEntry<T> {
	
	private final long length;
	
	
	public AbstractFileEntry(String name, T value, long length) {
		super(name, value);
		this.length = length;
	}
	

	public long getLength() {
		return length;
	}
	
}
