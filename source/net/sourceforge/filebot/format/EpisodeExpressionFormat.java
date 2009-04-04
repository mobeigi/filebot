
package net.sourceforge.filebot.format;


import java.io.File;

import javax.script.Bindings;
import javax.script.ScriptException;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.Episode;


public class EpisodeExpressionFormat extends ExpressionFormat {
	
	public EpisodeExpressionFormat(String format) throws ScriptException {
		super(format);
	}
	

	@Override
	public Bindings getBindings(Object value) {
		@SuppressWarnings("unchecked")
		Match<Episode, File> match = (Match<Episode, File>) value;
		
		return new ExpressionBindings(new EpisodeFormatBindingBean(match.getValue(), match.getCandidate()));
	}
	

	@Override
	protected void dispose(Bindings bindings) {
		// dispose binding bean
		getBindingBean(bindings).dispose();
	}
	

	private EpisodeFormatBindingBean getBindingBean(Bindings bindings) {
		return (EpisodeFormatBindingBean) ((ExpressionBindings) bindings).getBindingBean();
	}
	
}
