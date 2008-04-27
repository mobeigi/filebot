
package net.sourceforge.filebot.ui.panel.rename.entry;


import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.torrent.Torrent.Entry;


public class TorrentEntry extends AbstractFileEntry {
	
	private final Entry entry;
	
	
	public TorrentEntry(Entry entry) {
		super(FileFormat.getNameWithoutExtension(entry.getName()));
		
		this.entry = entry;
	}
	

	@Override
	public long getLength() {
		return entry.getLength();
	}
	
}
