
package net.sourceforge.filebot.ui.panel.rename.entry;


public abstract class AbstractFileEntry extends ListEntry {
	
	public AbstractFileEntry(String name) {
		super(name);
	}
	

	public abstract long getLength();
	
}
