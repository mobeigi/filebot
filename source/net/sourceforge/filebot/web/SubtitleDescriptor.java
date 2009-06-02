
package net.sourceforge.filebot.web;


import java.nio.ByteBuffer;

import javax.swing.SwingWorker;


public interface SubtitleDescriptor {
	
	public String getName();
	

	public String getLanguageName();
	

	public String getArchiveType();
	

	public SwingWorker<ByteBuffer, ?> createDownloadTask();
	
}
