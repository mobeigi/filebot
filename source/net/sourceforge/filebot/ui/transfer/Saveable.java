
package net.sourceforge.filebot.ui.transfer;


import java.io.File;


public interface Saveable {
	
	public abstract void save(File file);
	

	public abstract boolean isSaveable();
	

	public abstract String getDefaultFileName();
}
