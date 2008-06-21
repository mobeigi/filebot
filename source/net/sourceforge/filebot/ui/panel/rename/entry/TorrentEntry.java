
package net.sourceforge.filebot.ui.panel.rename.entry;


import net.sourceforge.filebot.torrent.Torrent.Entry;
import net.sourceforge.tuned.FileUtil;


public class TorrentEntry extends AbstractFileEntry {
	
	private final Entry entry;
	
	
	public TorrentEntry(Entry entry) {
		super(FileUtil.getNameWithoutExtension(entry.getName()));
		
		this.entry = entry;
	}
	

	@Override
	public long getLength() {
		return entry.getLength();
	}
	
}
