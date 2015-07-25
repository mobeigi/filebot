
package net.filebot.ui.rename;


import static net.filebot.util.FileUtilities.*;

import java.util.Formatter;
import java.util.Map;

import net.filebot.similarity.Match;
import net.filebot.web.MoviePart;


class MovieFormatter implements MatchFormatter {

	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof MoviePart;
	}


	@Override
	public String preview(Match<?, ?> match) {
		return format(match, null);
	}


	@Override
	public String format(Match<?, ?> match, Map<?, ?> context) {
		MoviePart video = (MoviePart) match.getValue();
		Formatter name = new Formatter(new StringBuilder());

		// format as single-file or multi-part movie
		name.format("%s (%d)", video.getName(), video.getYear());

		if (video.getPartCount() > 1) {
			name.format(".CD%d", video.getPartIndex());
		}

		// remove path separators if the name contains any / or \
		return replacePathSeparators(name.out().toString());
	}
}
