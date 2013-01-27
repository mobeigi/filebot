
package net.sourceforge.filebot.ui.rename;


import java.io.File;
import java.util.Map;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.tuned.FileUtilities;


class FileNameFormatter implements MatchFormatter {
	
	private boolean preserveExtension;
	
	
	public FileNameFormatter(boolean preserveExtension) {
		this.preserveExtension = preserveExtension;
	}
	
	
	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof File || match.getValue() instanceof FileInfo || match.getValue() instanceof String;
	}
	
	
	@Override
	public String preview(Match<?, ?> match) {
		return format(match, null);
	}
	
	
	@Override
	public String format(Match<?, ?> match, Map<?, ?> context) {
		Object value = match.getValue();
		
		if (value instanceof File) {
			File file = (File) value;
			return preserveExtension ? FileUtilities.getName(file) : file.getName();
		}
		
		if (value instanceof FileInfo) {
			FileInfo file = (FileInfo) value;
			return preserveExtension ? file.getName() : file.getPath();
		}
		
		if (value instanceof String) {
			return preserveExtension ? FileUtilities.getNameWithoutExtension(value.toString()) : value.toString();
		}
		
		// cannot format value
		throw new IllegalArgumentException("Illegal value: " + value);
	}
	
}
