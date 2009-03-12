
package net.sourceforge.filebot.ui.panel.episodelist;


import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;

import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.transfer.ArrayTransferable;
import net.sourceforge.filebot.ui.transfer.CompositeTranserable;
import net.sourceforge.filebot.web.Episode;


public class EpisodeListExportHandler extends FileBotListExportHandler {
	
	public EpisodeListExportHandler(FileBotList<Episode> list) {
		super(list);
	}
	

	@Override
	public Transferable createTransferable(JComponent c) {
		Transferable episodeArray = new ArrayTransferable<Episode>(list.getModel().toArray(new Episode[0]));
		Transferable textFile = super.createTransferable(c);
		
		return new CompositeTranserable(episodeArray, textFile);
	}
}
