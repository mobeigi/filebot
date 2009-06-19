
package net.sourceforge.filebot.web;


import java.nio.ByteBuffer;


public interface SubtitleDescriptor {
	
	String getName();
	

	String getLanguageName();
	

	String getType();
	

	ByteBuffer fetch() throws Exception;
	
}
