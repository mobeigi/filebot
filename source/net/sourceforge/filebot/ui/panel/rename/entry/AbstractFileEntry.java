
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class AbstractFileEntry<T> extends ListEntry<T> {
	
	public AbstractFileEntry(String name, T value) {
		super(name, value);
	}
	

	public abstract long getLength();
	
}
