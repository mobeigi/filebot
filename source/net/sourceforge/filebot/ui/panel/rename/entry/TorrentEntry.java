
package net.sourceforge.filebot.ui.panel.rename.entry;


import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.torrent.Torrent;
import net.sourceforge.filebot.torrent.Torrent.Entry;


public class TorrentEntry extends AbstractFileEntry<Torrent.Entry> {
	
	public TorrentEntry(Entry value) {
		super(value);
	}
	

	@Override
	public String getName(Torrent.Entry value) {
		return FileFormat.getNameWithoutSuffix(getValue().getName());
	}
	

	@Override
	public long getLength() {
		return getValue().getLength();
	}
	
}
