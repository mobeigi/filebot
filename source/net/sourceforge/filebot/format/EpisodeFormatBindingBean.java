
package net.sourceforge.filebot.format;


import static net.sourceforge.filebot.format.Define.undefined;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.FileBotUtilities;
import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.tuned.FileUtilities;


public class EpisodeFormatBindingBean {
	
	private final Episode episode;
	
	private final File mediaFile;
	
	private MediaInfo mediaInfo;
	
	
	public EpisodeFormatBindingBean(Episode episode, File mediaFile) {
		this.episode = episode;
		this.mediaFile = mediaFile;
	}
	

	@Define(undefined)
	public String undefined() {
		// omit expressions that depend on undefined values
		throw new RuntimeException("undefined");
	}
	

	@Define("n")
	public String getSeriesName() {
		return episode.getSeriesName();
	}
	

	@Define("s")
	public String getSeasonNumber() {
		return episode.getSeasonNumber();
	}
	

	@Define("e")
	public String getEpisodeNumber() {
		return episode.getEpisodeNumber();
	}
	

	@Define("t")
	public String getTitle() {
		return episode.getTitle();
	}
	

	@Define("vc")
	public String getVideoCodec() {
		return getMediaInfo(StreamKind.Video, 0, "Encoded_Library/Name", "CodecID/Hint", "Codec/String");
	}
	

	@Define("ac")
	public String getAudioCodec() {
		return getMediaInfo(StreamKind.Audio, 0, "CodecID/Hint", "Codec/String");
	}
	

	@Define("hi")
	public String getHeightAndInterlacement() {
		String height = getMediaInfo(StreamKind.Video, 0, "Height");
		String interlacement = getMediaInfo(StreamKind.Video, 0, "Interlacement");
		
		if (height == null || interlacement == null)
			return null;
		
		// e.g. 720p
		return height + Character.toLowerCase(interlacement.charAt(0));
	}
	

	@Define("resolution")
	public String getVideoResolution() {
		String width = getMediaInfo(StreamKind.Video, 0, "Width");
		String height = getMediaInfo(StreamKind.Video, 0, "Height");
		
		if (width == null || height == null)
			return null;
		
		// e.g. 1280x720
		return width + 'x' + height;
	}
	

	@Define("crc32")
	public String getCRC32() throws IOException {
		if (mediaFile != null) {
			// try to get checksum from file name
			String embeddedChecksum = FileBotUtilities.getEmbeddedChecksum(mediaFile.getName());
			
			if (embeddedChecksum != null) {
				return embeddedChecksum;
			}
			
			// calculate checksum from file
			return crc32(mediaFile);
		}
		
		return null;
	}
	

	@Define("ext")
	public String getExtension() {
		if (mediaFile != null) {
			return FileUtilities.getExtension(mediaFile);
		}
		
		return null;
	}
	

	@Define("general")
	public Object getGeneralMediaInfo() {
		return new AssociativeScriptObject(getMediaInfo().snapshot(StreamKind.General, 0));
	}
	

	@Define("video")
	public Object getVideoInfo() {
		return new AssociativeScriptObject(getMediaInfo().snapshot(StreamKind.Video, 0));
	}
	

	@Define("audio")
	public Object getAudioInfo() {
		return new AssociativeScriptObject(getMediaInfo().snapshot(StreamKind.Audio, 0));
	}
	

	@Define("text")
	public Object getTextInfo() {
		return new AssociativeScriptObject(getMediaInfo().snapshot(StreamKind.Text, 0));
	}
	

	@Define("image")
	public Object getImageInfo() {
		return new AssociativeScriptObject(getMediaInfo().snapshot(StreamKind.Image, 0));
	}
	

	public synchronized MediaInfo getMediaInfo() {
		if (mediaFile == null) {
			throw new NullPointerException("Media file is null");
		}
		
		if (mediaInfo == null) {
			mediaInfo = new MediaInfo();
			
			if (!mediaInfo.open(mediaFile)) {
				throw new RuntimeException(String.format("Cannot open file: %s", mediaFile));
			}
		}
		
		return mediaInfo;
	}
	

	public synchronized void dispose() {
		if (mediaInfo != null) {
			mediaInfo.close();
			mediaInfo.dispose();
		}
		
		mediaInfo = null;
	}
	

	private String getMediaInfo(StreamKind streamKind, int streamNumber, String... keys) {
		for (String key : keys) {
			String value = getMediaInfo().get(streamKind, streamNumber, key);
			
			if (value.length() > 0) {
				return value;
			}
		}
		
		return null;
	}
	
	private static final Cache checksumCache = CacheManager.getInstance().getCache("checksum");
	
	
	private String crc32(File file) throws IOException {
		// try to get checksum from cache
		Element cacheEntry = checksumCache.get(file);
		
		if (cacheEntry != null) {
			return (String) cacheEntry.getValue();
		}
		
		// calculate checksum
		InputStream in = new FileInputStream(file);
		CRC32 crc = new CRC32();
		
		try {
			byte[] buffer = new byte[32 * 1024];
			int len = 0;
			
			while ((len = in.read(buffer)) >= 0) {
				crc.update(buffer, 0, len);
			}
		} finally {
			in.close();
		}
		
		String checksum = String.format("%08X", crc.getValue());
		
		checksumCache.put(new Element(file, checksum));
		return checksum;
	}
	
}
