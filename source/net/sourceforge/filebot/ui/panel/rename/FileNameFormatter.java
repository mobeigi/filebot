
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.FileUtilities;


class FileNameFormatter implements MatchFormatter {
	
	private final boolean preserveExtension;
	

	public FileNameFormatter(boolean preserveExtension) {
		this.preserveExtension = preserveExtension;
	}
	

	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof File || match.getValue() instanceof AbstractFile;
	}
	

	@Override
	public String preview(Match<?, ?> match) {
		return format(match);
	}
	

	@Override
	public String format(Match<?, ?> match) {
		if (match.getValue() instanceof File) {
			File file = (File) match.getValue();
			return preserveExtension ? FileUtilities.getName(file) : file.getName();
		}
		
		if (match.getValue() instanceof AbstractFile) {
			AbstractFile file = (AbstractFile) match.getValue();
			return preserveExtension ? FileUtilities.getNameWithoutExtension(file.getName()) : file.getName();
		}
		
		// cannot format value
		throw new IllegalArgumentException("Illegal value: " + match.getValue());
	}
	
}
