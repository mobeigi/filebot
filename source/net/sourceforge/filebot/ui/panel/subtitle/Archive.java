
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;


interface Archive {
	
	Map<File, ByteBuffer> extract() throws IOException;
	
}
