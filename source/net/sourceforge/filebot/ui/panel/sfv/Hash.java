
package net.sourceforge.filebot.ui.panel.sfv;


interface Hash {
	
	public void update(byte[] bytes, int off, int len);
	

	public String digest();
	
}
