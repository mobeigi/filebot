
package net.sourceforge.filebot.format;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.format.Define.*;
import static net.sourceforge.filebot.hash.VerificationUtilities.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.zip.CRC32;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.tuned.FileUtilities;


public class EpisodeBindingBean {
	
	private final Episode episode;
	
	private final File mediaFile;
	
	private MediaInfo mediaInfo;
	

	public EpisodeBindingBean(Episode episode, File mediaFile) {
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
	public Integer getSeasonNumber() {
		return episode.getSeason();
	}
	

	@Define("e")
	public Integer getEpisodeNumber() {
		return episode.getEpisode();
	}
	

	@Define("t")
	public String getTitle() {
		return episode.getTitle();
	}
	

	@Define("airdate")
	public Date airdate() {
		return episode.airdate();
	}
	

	@Define("absolute")
	public Integer getAbsoluteEpisodeNumber() {
		return episode.getAbsolute();
	}
	

	@Define("special")
	public Integer getSpecialNumber() {
		return episode.getSpecial();
	}
	

	@Define("vc")
	public String getVideoCodec() {
		// e.g. XviD, x264, DivX 5, MPEG-4 Visual, AVC, etc.
		String codec = getMediaInfo(StreamKind.Video, 0, "Encoded_Library/Name", "CodecID/Hint", "Format");
		
		// get first token (e.g. DivX 5 => DivX)
		return new Scanner(codec).next();
	}
	

	@Define("ac")
	public String getAudioCodec() {
		// e.g. AC-3, DTS, AAC, Vorbis, MP3, etc.
		String codec = getMediaInfo(StreamKind.Audio, 0, "CodecID/Hint", "Format");
		
		// remove punctuation (e.g. AC-3 => AC3)
		return codec.replaceAll("\\p{Punct}", "");
	}
	

	@Define("cf")
	public String getContainerFormat() {
		// container format extensions (e.g. avi, mkv mka mks, OGG, etc.)
		String extensions = getMediaInfo(StreamKind.General, 0, "Codec/Extensions", "Format");
		
		// get first extension
		return new Scanner(extensions).next().toLowerCase();
	}
	

	@Define("vf")
	public String getVideoFormat() {
		String height = getMediaInfo(StreamKind.Video, 0, "Height");
		String scanType = getMediaInfo(StreamKind.Video, 0, "ScanType");
		
		if (height == null || scanType == null)
			return null;
		
		// e.g. 720p
		return height + Character.toLowerCase(scanType.charAt(0));
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
	public String getCRC32() throws IOException, InterruptedException {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();
		
		// try to get checksum from file name
		String checksum = getEmbeddedChecksum(inferredMediaFile.getName());
		
		if (checksum != null)
			return checksum;
		
		// try to get checksum from sfv file
		checksum = getHashFromVerificationFile(inferredMediaFile, HashType.SFV, 3);
		
		if (checksum != null)
			return checksum;
		
		// calculate checksum from file
		return crc32(inferredMediaFile);
	}
	

	@Define("fn")
	public String getFileName() {
		// make sure media file is defined
		checkMediaFile();
		
		// file extension
		return FileUtilities.getName(mediaFile);
	}
	

	@Define("ext")
	public String getExtension() {
		// make sure media file is defined
		checkMediaFile();
		
		// file extension
		return FileUtilities.getExtension(mediaFile);
	}
	

	@Define("media")
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
	

	@Define("episode")
	public Episode getEpisode() {
		return episode;
	}
	

	@Define("file")
	public File getMediaFile() {
		return mediaFile;
	}
	

	private File getInferredMediaFile() {
		// make sure media file is defined
		checkMediaFile();
		
		if (SUBTITLE_FILES.accept(mediaFile)) {
			// file is a subtitle
			String name = FileUtilities.getName(mediaFile);
			
			// find corresponding movie file
			for (File movie : mediaFile.getParentFile().listFiles(VIDEO_FILES)) {
				if (name.startsWith(FileUtilities.getName(movie))) {
					return movie;
				}
			}
		}
		
		return mediaFile;
	}
	

	private void checkMediaFile() throws RuntimeException {
		// make sure file is not null, and that it is an existing file
		if (mediaFile == null || !mediaFile.isFile())
			throw new RuntimeException(String.format("Invalid media file: %s", mediaFile));
	}
	

	private synchronized MediaInfo getMediaInfo() {
		if (mediaInfo == null) {
			// make sure media file is defined
			checkMediaFile();
			
			MediaInfo newMediaInfo = new MediaInfo();
			
			// use inferred media file (e.g. actual movie file instead of subtitle file)
			if (!newMediaInfo.open(getInferredMediaFile())) {
				throw new RuntimeException(String.format("Cannot open media file: %s", mediaFile));
			}
			
			mediaInfo = newMediaInfo;
		}
		
		return mediaInfo;
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
	

	private String crc32(File file) throws IOException, InterruptedException {
		// try to get checksum from cache
		Cache cache = CacheManager.getInstance().getCache("checksum");
		
		try {
			return String.format("%08X", cache.get(file).getValue());
		} catch (Exception e) {
			// checksum is not cached
		}
		
		// calculate checksum
		InputStream in = new FileInputStream(file);
		CRC32 crc = new CRC32();
		
		try {
			byte[] buffer = new byte[32 * 1024];
			int len = 0;
			
			while ((len = in.read(buffer)) >= 0) {
				crc.update(buffer, 0, len);
				
				// make this long-running operation interruptible
				if (Thread.interrupted())
					throw new InterruptedException();
			}
		} finally {
			in.close();
		}
		
		// cache calculated checksum 
		cache.put(new Element(file, crc.getValue()));
		
		return String.format("%08X", crc.getValue());
	}
	
}
