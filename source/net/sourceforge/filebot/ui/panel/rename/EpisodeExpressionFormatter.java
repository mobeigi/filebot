
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;

import javax.script.ScriptException;

import net.sourceforge.filebot.format.EpisodeBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;


class EpisodeExpressionFormatter implements MatchFormatter {
	
	private final String expression;
	
	private ExpressionFormat format;
	

	public EpisodeExpressionFormatter(String expression) {
		if (expression == null || expression.isEmpty())
			throw new IllegalArgumentException("Expression must not be null or empty");
		
		this.expression = expression;
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
		
		// lazy initialize script engine
		if (format == null) {
			format = new ExpressionFormat(expression);
		}
		
		String result = format.format(new EpisodeBindingBean(episode, mediaFile)).trim();
		
		// if result is empty, check for script exceptions
		if (result.isEmpty() && format.caughtScriptException() != null) {
			throw format.caughtScriptException();
		}
		
		return result;
	}
	
}
