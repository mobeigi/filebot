
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;

import javax.script.ScriptException;

import net.sourceforge.filebot.format.EpisodeFormatBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;


class EpisodeExpressionFormatter implements MatchFormatter {
	
	private final ExpressionFormat format;
	

	public EpisodeExpressionFormatter(ExpressionFormat format) {
		this.format = format;
	}
	

	public ExpressionFormat getFormat() {
		return format;
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
	public synchronized String format(Match<?, ?> match) throws ScriptException {
		Episode episode = (Episode) match.getValue();
		File mediaFile = (File) match.getCandidate();
		
		String result = format.format(new EpisodeFormatBindingBean(episode, mediaFile)).trim();
		
		// if result is empty, check for script exceptions
		if (result.isEmpty() && format.caughtScriptException() != null) {
			throw format.caughtScriptException();
		}
		
		return result;
	}
	
}
