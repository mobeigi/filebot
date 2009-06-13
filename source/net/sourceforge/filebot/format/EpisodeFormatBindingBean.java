
package net.sourceforge.filebot.format;


import static net.sourceforge.filebot.FileBotUtilities.*;
import static net.sourceforge.filebot.format.Define.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.zip.CRC32;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.FileBotUtilities;
import net.sourceforge.filebot.hash.SfvFormat;
import net.sourceforge.filebot.hash.VerificationFileScanner;
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
		return episode.getSeason();
	}
	

	@Define("e")
	public String getEpisodeNumber() {
		return episode.getEpisode();
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
	

	@Define("cf")
	public String getContainerFormat() {
		// container format extension 
		String extensions = getMediaInfo(StreamKind.General, 0, "Codec/Extensions");
		
		// get first token
		return new Scanner(extensions).next();
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
	public String getCRC32() throws IOException, InterruptedException {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();
		
		// try to get checksum from file name
		String checksum = FileBotUtilities.getEmbeddedChecksum(inferredMediaFile.getName());
		
		if (checksum != null)
			return checksum;
		
		// try to get checksum from sfv file
		checksum = getChecksumFromSfvFile(inferredMediaFile);
		
		if (checksum != null)
			return checksum;
		
		// calculate checksum from file
		return crc32(inferredMediaFile);
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
	

	@Define("image")
	public Object getImageInfo() {
		return new AssociativeScriptObject(getMediaInfo().snapshot(StreamKind.Image, 0));
	}
	

	@Define("episode")
	public Episode getEpisode() {
		return episode;
	}
	

	@Define("file")
	public File getMediaFile() {
		return mediaFile;
	}
	

	@Define("inferredFile")
	public File getInferredMediaFile() {
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
	

	private void checkMediaFile() {
		// make sure file is not null
		if (mediaFile == null)
			throw new NullPointerException("Media file is not defined");
		
		// file may not exist at this point but if an existing file is required, 
		// an exception will be thrown later anyway
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
	

	private String getChecksumFromSfvFile(File file) throws IOException {
		File folder = file.getParentFile();
		
		for (File sfvFile : folder.listFiles(SFV_FILES)) {
			VerificationFileScanner scanner = new VerificationFileScanner(sfvFile, new SfvFormat());
			
			try {
				while (scanner.hasNext()) {
					Entry<File, String> entry = scanner.next();
					
					if (file.getName().equals(entry.getKey().getPath())) {
						return entry.getValue();
					}
				}
			} finally {
				scanner.close();
			}
		}
		
		return null;
	}
	

	private String crc32(File file) throws IOException, InterruptedException {
		// try to get checksum from cache
		Cache cache = CacheManager.getInstance().getCache("checksum");
		
		Element cacheEntry = cache.get(file);
		
		if (cacheEntry != null) {
			return String.format("%08X", cacheEntry.getValue());
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
