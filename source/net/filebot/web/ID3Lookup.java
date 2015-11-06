package net.filebot.web;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;

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
	public Map<File, AudioTrack> lookup(Collection<File> files) throws Exception {
		Map<File, AudioTrack> info = new LinkedHashMap<File, AudioTrack>();

		MediaInfo mediaInfo = new MediaInfo();
		for (File f : files) {
			if (!mediaInfo.open(f)) {
				throw new IOException("MediaInfo failed to open file: " + f);
			}

			try {
				// artist and song title information is required
				String artist = getString(mediaInfo, "Performer");
				String title = getString(mediaInfo, "Title");

				if (artist != null && title != null) {
					// all other properties are optional
					String album = getString(mediaInfo, "Album");
					String albumArtist = getString(mediaInfo, "Album/Performer");
					String trackTitle = null;
					SimpleDate albumReleaseDate = null;
					Integer mediumIndex = null;
					Integer mediumCount = null;
					Integer trackIndex = getInteger(mediaInfo, "Track/Position");
					Integer trackCount = getInteger(mediaInfo, "Track/Position_Total");
					String mbid = null;

					Integer year = getInteger(mediaInfo, "Recorded_Date");
					if (year != null) {
						albumReleaseDate = new SimpleDate(year, 1, 1);
					}

					info.put(f, new AudioTrack(artist, title, album, albumArtist, trackTitle, albumReleaseDate, mediumIndex, mediumCount, trackIndex, trackCount, mbid));
				}
			} catch (Throwable e) {
				Logger.getLogger(ID3Lookup.class.getName()).log(Level.WARNING, e.toString());
			} finally {
				mediaInfo.close();
			}
		}

		return info;
	}

	private String getString(MediaInfo mediaInfo, String field) {
		String value = mediaInfo.get(StreamKind.General, 0, field).trim();
		if (value.length() > 0) {
			return value;
		}
		return null;
	}

	private Integer getInteger(MediaInfo mediaInfo, String field) {
		String value = getString(mediaInfo, field);
		if (value != null) {
			try {
				return new Integer(value);
			} catch (Exception e) {
				Logger.getLogger(ID3Lookup.class.getName()).log(Level.WARNING, e.toString());
			}
		}
		return null;
	}

}
