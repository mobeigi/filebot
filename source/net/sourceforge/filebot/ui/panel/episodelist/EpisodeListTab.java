
package net.sourceforge.filebot.ui.panel.episodelist;


import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotTab;
import net.sourceforge.filebot.web.Episode;


public class EpisodeListTab extends FileBotTab<FileBotList<Episode>> {
	
	public EpisodeListTab() {
		super(new FileBotList<Episode>());
		
		// set export handler for episode list
		getComponent().setExportHandler(new FileBotListExportHandler(getComponent()));
		
		// allow removal of episode list entries
		getComponent().getRemoveAction().setEnabled(true);
	}
	
}
