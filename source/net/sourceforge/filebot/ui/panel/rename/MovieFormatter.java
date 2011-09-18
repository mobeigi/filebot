
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.tuned.FileUtilities.*;

import java.util.Formatter;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.MoviePart;


class MovieFormatter implements MatchFormatter {
	
	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof MoviePart;
	}
	

	@Override
	public String preview(Match<?, ?> match) {
		return format(match);
	}
	

	@Override
	public String format(Match<?, ?> match) {
		MoviePart video = (MoviePart) match.getValue();
		Formatter name = new Formatter(new StringBuilder());
		
		// format as single-file or multi-part movie
		name.format("%s (%d)", video.getName(), video.getYear());
		
		if (video.getPartCount() > 1)
			name.format(" CD%d", video.getPartIndex() + 1);
		
		// remove path separators if the name contains any / or \
		return replacePathSeparators(name.out().toString());
	}
}
