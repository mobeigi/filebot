
package net.sourceforge.filebot.ui.panel.rename;


class MutableString {
	
	private String value;
	
	
	public MutableString(Object value) {
		set(value);
	}
	

	public void set(Object value) {
		this.value = String.valueOf(value);
	}
	

	@Override
	public String toString() {
		return value;
	}
	
}
