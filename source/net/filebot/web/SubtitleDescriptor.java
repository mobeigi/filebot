
package net.filebot.web;


import java.nio.ByteBuffer;

import net.filebot.vfs.FileInfo;


public interface SubtitleDescriptor extends FileInfo {
	
	String getName();
	

	String getLanguageName();
	

	String getType();
	

	ByteBuffer fetch() throws Exception;
	
}
