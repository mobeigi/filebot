
package net.sourceforge.filebot.ui.panel.rename.entry;


public class ListEntry<T> {
	
	private String name;
	private T value;
	
	
	public ListEntry(String name, T value) {
		this.name = name;
		this.value = value;
	}
	

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
