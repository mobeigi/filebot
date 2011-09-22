
package net.sourceforge.filebot.ui.rename;


import net.sourceforge.filebot.similarity.Match;


public interface MatchFormatter {
	
	public boolean canFormat(Match<?, ?> match);
	

	public String preview(Match<?, ?> match);
	

	public String format(Match<?, ?> match) throws Exception;
	
}
