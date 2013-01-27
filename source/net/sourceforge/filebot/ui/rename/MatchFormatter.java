
package net.sourceforge.filebot.ui.rename;


import java.util.Map;

import net.sourceforge.filebot.similarity.Match;


public interface MatchFormatter {
	
	public boolean canFormat(Match<?, ?> match);
	
	
	public String preview(Match<?, ?> match);
	
	
	public String format(Match<?, ?> match, Map<?, ?> context) throws Exception;
	
}
