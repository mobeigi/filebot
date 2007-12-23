
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class AbstractFileEntry<T> extends ListEntry<T> {
	
	public AbstractFileEntry(T value) {
		super(value);
	}
	

	@Override
	public String toString() {
		return getName();
	}
	

	public abstract long getLength();
	

	public abstract String getName();
}
