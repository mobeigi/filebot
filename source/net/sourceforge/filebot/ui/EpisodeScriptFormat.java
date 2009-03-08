
package net.sourceforge.filebot.ui;


import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import net.sourceforge.filebot.web.Episode;


public class EpisodeScriptFormat extends ScriptFormat {
	
	public EpisodeScriptFormat(String format) throws ScriptException {
		super(format);
	}
	

	@Override
	protected Bindings getBindings(Object object) {
		Episode episode = (Episode) object;
		
		Bindings bindings = new SimpleBindings();
		
		bindings.put("n", episode.getSeriesName());
		bindings.put("s", episode.getSeasonNumber());
		bindings.put("e", episode.getEpisodeNumber());
		bindings.put("t", episode.getTitle());
		
		return bindings;
	}
}
