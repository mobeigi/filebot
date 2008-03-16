
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class ListEntry<T> {
	
	private String name;
	private T value;
	
	
	public ListEntry(T value) {
		this.value = value;
		this.name = getName(value);
	}
	

	protected abstract String getName(T value);
	

	public String getName() {
		return name;
	}
	

	public void setName(String name) {
		this.name = name;
	}
	

	public T getValue() {
		return value;
	}
	

	@Override
	public String toString() {
		return getName();
	}
}
