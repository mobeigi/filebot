package net.sourceforge.filebot.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

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

			try {
				String artist = mediaInfo.get(StreamKind.General, 0, "Performer");
				String title = mediaInfo.get(StreamKind.General, 0, "Title");
				String album = mediaInfo.get(StreamKind.General, 0, "Album");

				// extra info if available
				String albumArtist = null, trackTitle = null;
				Date albumReleaseDate = null;
				Integer mediumIndex = null, mediumCount = null, trackIndex = null, trackCount = null;

				try {
					int year = new Scanner(mediaInfo.get(StreamKind.General, 0, "Recorded_Date")).useDelimiter("\\D+").nextInt();
					albumReleaseDate = new Date(year, 1, 1);
				} catch (Exception e) {
					// ignore
				}

				try {
					trackIndex = Integer.parseInt(mediaInfo.get(StreamKind.General, 0, "Track/Position"));
				} catch (Exception e) {
					// ignore
				}

				try {
					trackCount = Integer.parseInt(mediaInfo.get(StreamKind.General, 0, "Track/Position_Total"));
				} catch (Exception e) {
					// ignore
				}

				if (artist.length() > 0 && title.length() > 0 && album.length() > 0) {
					info.put(f, new AudioTrack(artist, title, album, albumArtist, trackTitle, albumReleaseDate, mediumIndex, mediumCount, trackIndex, trackCount));
				}
			} catch (Throwable e) {
				Logger.getLogger(ID3Lookup.class.getName()).log(Level.WARNING, e.getMessage(), e);
			} finally {
				mediaInfo.close();
			}
		}

		return info;
	}

}
