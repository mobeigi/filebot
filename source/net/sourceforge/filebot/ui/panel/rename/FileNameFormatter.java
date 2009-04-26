
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.FileUtilities;


public class FileNameFormatter implements MatchFormatter {
	
	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof File;
	}
	

	@Override
	public String preview(Match<?, ?> match) {
		return format(match);
	}
	

	@Override
	public String format(Match<?, ?> match) {
		File file = (File) match.getValue();
		
		return FileUtilities.getName(file);
	}
	
}
