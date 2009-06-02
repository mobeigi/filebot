
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;


interface Archive {
	
	Map<String, ByteBuffer> extract() throws IOException;
	
}
