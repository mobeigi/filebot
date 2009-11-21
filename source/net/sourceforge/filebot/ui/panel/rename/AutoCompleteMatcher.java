
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.List;

import net.sourceforge.filebot.similarity.Match;


interface AutoCompleteMatcher {
	
	List<Match<File, ?>> match(List<File> files) throws Exception;
}
