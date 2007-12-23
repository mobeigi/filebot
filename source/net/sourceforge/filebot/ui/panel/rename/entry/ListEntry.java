
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class ListEntry<T> {
	
	private T value;
	
	
	public ListEntry(T value) {
		this.value = value;
	}
	

	public T getValue() {
		return value;
	}
	

	@Override
	public abstract String toString();
	
}
