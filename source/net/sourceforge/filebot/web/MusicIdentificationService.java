
package net.sourceforge.filebot.web;


import java.io.File;
import java.util.Map;

import javax.swing.Icon;


public interface MusicIdentificationService {
	
	String getName();
	
	
	Icon getIcon();
	
	
	Map<File, AudioTrack> lookup(Iterable<File> files) throws Exception;
	
}
