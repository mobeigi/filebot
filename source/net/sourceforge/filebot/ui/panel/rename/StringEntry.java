
package net.sourceforge.filebot.ui.panel.rename;


class StringEntry {
	
	private String value;
	
	
	public StringEntry(Object value) {
		setValue(value);
	}
	

	public String getValue() {
		return value;
	}
	

	public void setValue(Object value) {
		this.value = String.valueOf(value);
	}
	

	@Override
	public String toString() {
		return getValue();
	}
	
}
