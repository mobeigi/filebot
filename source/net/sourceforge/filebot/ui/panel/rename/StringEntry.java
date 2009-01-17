
package net.sourceforge.filebot.ui.panel.rename;


class StringEntry {
	
	private String value;
	
	
	public StringEntry(String value) {
		this.value = value;
	}
	

	public String getValue() {
		return value;
	}
	

	public void setValue(String value) {
		this.value = value;
	}
	

	@Override
	public String toString() {
		return getValue();
	}
	
}
