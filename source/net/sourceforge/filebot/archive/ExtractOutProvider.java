
package net.sourceforge.filebot.archive;


import java.io.*;


public interface ExtractOutProvider {
	
	OutputStream getStream(File archivePath) throws IOException;
	
}
