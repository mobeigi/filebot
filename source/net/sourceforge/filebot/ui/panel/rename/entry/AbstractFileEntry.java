
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class AbstractFileEntry<T> extends ListEntry<T> {
	
	public AbstractFileEntry(T value) {
		super(value);
	}
	

	public abstract long getLength();
	
}
