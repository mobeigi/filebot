
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;

import javax.script.ScriptException;

import net.sourceforge.filebot.format.EpisodeFormatBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;


class EpisodeExpressionFormatter extends ExpressionFormat implements MatchFormatter {
	
	public EpisodeExpressionFormatter(String expression) throws ScriptException {
		super(expression);
	}
	

	@Override
	public boolean canFormat(Match<?, ?> match) {
		// episode is required, file is optional
		return match.getValue() instanceof Episode && (match.getCandidate() == null || match.getCandidate() instanceof File);
	}
	

	@Override
	public String preview(Match<?, ?> match) {
		return EpisodeFormat.getInstance().format(match.getValue());
	}
	

	@Override
	public String format(Match<?, ?> match) {
		Episode episode = (Episode) match.getValue();
		File mediaFile = (File) match.getCandidate();
		
		return format(new EpisodeFormatBindingBean(episode, mediaFile)).trim();
	}
	
}
