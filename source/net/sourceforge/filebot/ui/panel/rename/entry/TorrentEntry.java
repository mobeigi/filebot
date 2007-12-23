
package net.sourceforge.filebot.ui.panel.rename.entry;


import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.torrent.Torrent.Entry;
import net.sourceforge.filebot.ui.FileFormat;


public class TorrentEntry extends AbstractFileEntry<Torrent.Entry> {
	
	public TorrentEntry(Entry value) {
		super(value);
	}
	

	@Override
	public String getName() {
		return FileFormat.getNameWithoutSuffix(getValue().getName());
	}
	

	@Override
	public long getLength() {
		return getValue().getLength();
	}
	
}
