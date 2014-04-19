
package net.sourceforge.filebot.web;


import java.nio.ByteBuffer;

import net.sourceforge.filebot.vfs.FileInfo;


public interface SubtitleDescriptor extends FileInfo {
	
	String getName();
	

	String getLanguageName();
	

	String getType();
	

	ByteBuffer fetch() throws Exception;
	
}
