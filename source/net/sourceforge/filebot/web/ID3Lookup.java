package net.sourceforge.filebot.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;

public class ID3Lookup implements MusicIdentificationService {

	@Override
	public String getName() {
		return "ID3 Tags";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.mediainfo");
	}

	@Override
	public Map<File, AudioTrack> lookup(Iterable<File> files) throws Exception {
		Map<File, AudioTrack> info = new HashMap<File, AudioTrack>();

		MediaInfo mediaInfo = new MediaInfo();
		for (File f : files) {
			if (!mediaInfo.open(f)) {
				throw new IOException("MediaInfo failed to open file: " + f);
			}

			String artist = mediaInfo.get(StreamKind.General, 0, "Performer");
			String title = mediaInfo.get(StreamKind.General, 0, "Title");
			String album = mediaInfo.get(StreamKind.General, 0, "Album");
			mediaInfo.close();

			info.put(f, new AudioTrack(artist, title, album));
		}

		return info;
	}

}
