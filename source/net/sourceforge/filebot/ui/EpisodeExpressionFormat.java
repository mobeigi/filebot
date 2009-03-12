
package net.sourceforge.filebot.ui;


import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import net.sourceforge.filebot.web.Episode;


public class EpisodeExpressionFormat extends ExpressionFormat {
	
	public EpisodeExpressionFormat(String format) throws ScriptException {
		super(format);
	}
	

	@Override
	public Bindings getBindings(Object value) {
		Episode episode = (Episode) value;
		
		Bindings bindings = new SimpleBindings();
		
		bindings.put("n", nonNull(episode.getSeriesName()));
		bindings.put("s", nonNull(episode.getSeasonNumber()));
		bindings.put("e", nonNull(episode.getEpisodeNumber()));
		bindings.put("t", nonNull(episode.getTitle()));
		
		return bindings;
	}
	

	private String nonNull(String value) {
		return value == null ? "" : value;
	}
	
}
