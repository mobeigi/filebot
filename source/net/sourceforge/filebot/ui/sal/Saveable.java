
package net.sourceforge.filebot.ui.sal;


import java.io.File;


public interface Saveable {
	
	public void save(File file);
	

	public boolean isSaveable();
	

	public String getDefaultFileName();
}
