package net.filebot.web;

import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.StringUtilities.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;
import net.filebot.mediainfo.MediaInfoException;

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

		try (MediaInfo mediaInfo = new MediaInfo()) {
			for (File f : filter(files, AUDIO_FILES, VIDEO_FILES)) {
				try {
					if (!mediaInfo.open(f)) {
						throw new MediaInfoException("Failed to read media info: " + f);
					}

					// artist and song title information is required
					String artist = getString(mediaInfo, "Performer", "Composer");
					String title = getString(mediaInfo, "Title", "Track");

					if (artist != null && title != null) {
						// all other properties are optional
						String album = getString(mediaInfo, "Album");
						String albumArtist = getString(mediaInfo, "Album/Performer");
						String trackTitle = getString(mediaInfo, "Track");
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
					debug.warning(e.getMessage());
				}
			}
		}

		return info;
	}

	private String getString(MediaInfo mediaInfo, String... keys) {
		for (String key : keys) {
			String value = mediaInfo.get(StreamKind.General, 0, key);
			if (value.length() > 0) {
				return value;
			}
		}
		return null;
	}

	private Integer getInteger(MediaInfo mediaInfo, String field) {
		return matchInteger(getString(mediaInfo, field));
	}

}
