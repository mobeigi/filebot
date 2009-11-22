
package net.sourceforge.filebot.ui.panel.rename;


import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.MovieDescriptor;


class MovieFormatter implements MatchFormatter {
	
	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof MovieDescriptor;
	}
	

	@Override
	public String preview(Match<?, ?> match) {
		return format(match);
	}
	

	@Override
	public String format(Match<?, ?> match) {
		// use default format for the time being
		return match.getValue().toString();
	}
	
}
