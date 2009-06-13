
package net.sourceforge.filebot.web;


import java.nio.ByteBuffer;
import java.util.concurrent.Callable;


public interface SubtitleDescriptor {
	
	String getName();
	

	String getLanguageName();
	

	String getArchiveType();
	

	Callable<ByteBuffer> getDownloadFunction();
	
}
