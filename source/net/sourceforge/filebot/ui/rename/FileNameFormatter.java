
package net.sourceforge.filebot.ui.rename;


import java.io.File;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.tuned.FileUtilities;


class FileNameFormatter implements MatchFormatter {
	
	private final boolean preserveExtension;
	

	public FileNameFormatter(boolean preserveExtension) {
		this.preserveExtension = preserveExtension;
	}
	

	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof File || match.getValue() instanceof FileInfo;
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
		
		if (match.getValue() instanceof FileInfo) {
			FileInfo file = (FileInfo) match.getValue();
			return preserveExtension ? file.getName() : file.getPath();
		}
		
		// cannot format value
		throw new IllegalArgumentException("Illegal value: " + match.getValue());
	}
	
}
