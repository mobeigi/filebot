
package net.sourceforge.filebot.format;


import static java.util.Arrays.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.format.Define.*;
import static net.sourceforge.filebot.hash.VerificationUtilities.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.filebot.web.EpisodeFormat.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;
import net.sourceforge.filebot.web.Date;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MoviePart;
import net.sourceforge.filebot.web.MultiEpisode;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.tuned.FileUtilities;


public class MediaBindingBean {
	
	private final Object infoObject;
	private final File mediaFile;
	private MediaInfo mediaInfo;
	private Object metaInfo;
	
	
	public MediaBindingBean(Object infoObject, File mediaFile) {
		this.infoObject = infoObject;
		this.mediaFile = mediaFile;
	}
	
	
	@Define(undefined)
	public <T> T undefined() {
		// omit expressions that depend on undefined values
		throw new RuntimeException("undefined");
	}
	
	
	@Define("n")
	public String getName() {
		if (infoObject instanceof Episode)
			return getEpisode().getSeriesName();
		if (infoObject instanceof Movie)
			return getMovie().getName();
		
		return null;
	}
	
	
	@Define("y")
	public Integer getYear() {
		if (infoObject instanceof Episode)
			return getEpisode().getSeriesStartDate().getYear();
		if (infoObject instanceof Movie)
			return getMovie().getYear();
		
		return null;
	}
	
	
	@Define("s")
	public Integer getSeasonNumber() {
		return getEpisode().getSeason();
	}
	
	
	@Define("e")
	public Integer getEpisodeNumber() {
		return getEpisode().getEpisode();
	}
	
	
	@Define("sxe")
	public String getSxE() {
		return SeasonEpisode.formatSxE(getEpisode());
	}
	
	
	@Define("s00e00")
	public String getS00E00() {
		return SeasonEpisode.formatS00E00(getEpisode());
	}
	
	
	@Define("t")
	public String getTitle() {
		// single episode format
		if (getEpisodes().size() == 1) {
			return getEpisode().getTitle();
		}
		
		// multi-episode format
		Set<String> title = new LinkedHashSet<String>();
		for (Episode it : getEpisodes()) {
			title.add(removeTrailingBrackets(it.getTitle()));
		}
		return join(title, " & ");
	}
	
	
	@Define("airdate")
	public Date airdate() {
		return getEpisode().airdate();
	}
	
	
	@Define("startdate")
	public Date startdate() {
		return getEpisode().getSeriesStartDate();
	}
	
	
	@Define("absolute")
	public Integer getAbsoluteEpisodeNumber() {
		return getEpisode().getAbsolute();
	}
	
	
	@Define("special")
	public Integer getSpecialNumber() {
		return getEpisode().getSpecial();
	}
	
	
	@Define("imdb")
	public String getImdbId() {
		int imdb = getMovie().getImdbId();
		
		if (imdb <= 0)
			return null;
		
		return String.format("%07d", imdb);
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
		// {def h = video.height as int; h > 720 ? 1080 : h > 480 ? 720 : 480}p
		int height = Integer.parseInt(getMediaInfo(StreamKind.Video, 0, "Height"));
		String vf = "";
		
		if (height > 720)
			vf += 1080;
		else if (height > 480)
			vf += 720;
		else if (height > 360)
			vf += 480;
		else
			return null; // video too small
			
		// e.g. 720p
		return vf + 'p'; // nobody actually wants files to be tagged as interlaced, e.g. 720i
	}
	
	
	@Define("hpi")
	public String getExactVideoFormat() {
		String height = getMediaInfo(StreamKind.Video, 0, "Height");
		String scanType = getMediaInfo(StreamKind.Video, 0, "ScanType");
		
		if (height == null || scanType == null)
			return null;
		
		// e.g. 720p
		return height + Character.toLowerCase(scanType.charAt(0));
	}
	
	
	@Define("af")
	public String getAudioChannels() {
		String channels = getMediaInfo(StreamKind.Audio, 0, "Channel(s)");
		
		if (channels == null)
			return null;
		
		// e.g. 6ch
		return channels + "ch";
	}
	
	
	@Define("resolution")
	public String getVideoResolution() {
		List<Integer> dim = getDimension();
		
		if (dim.contains(null))
			return null;
		
		// e.g. 1280x720
		return join(dim, "x");
	}
	
	
	@Define("ws")
	public String getWidescreen() {
		List<Integer> dim = getDimension();
		
		// width-to-height aspect ratio greater than 1.37:1
		return (float) dim.get(0) / dim.get(1) > 1.37f ? "ws" : null;
	}
	
	
	@Define("sdhd")
	public String getVideoDefinitionCategory() {
		List<Integer> dim = getDimension();
		
		// SD (less than 720 lines) or HD (more than 720 lines)
		return dim.get(0) >= 1280 || dim.get(1) >= 720 ? "HD" : "SD";
	}
	
	
	@Define("dim")
	public List<Integer> getDimension() {
		String width = getMediaInfo(StreamKind.Video, 0, "Width");
		String height = getMediaInfo(StreamKind.Video, 0, "Height");
		
		return Arrays.asList(width != null ? Integer.parseInt(width) : null, height != null ? Integer.parseInt(height) : null);
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
	
	
	@Define("source")
	public String getVideoSource() {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();
		
		// look for video source patterns in media file and it's parent folder
		return releaseInfo.getVideoSource(inferredMediaFile);
	}
	
	
	@Define("group")
	public String getReleaseGroup() throws IOException {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();
		
		// look for release group names in media file and it's parent folder
		return releaseInfo.getReleaseGroup(inferredMediaFile);
	}
	
	
	@Define("lang")
	public Locale detectSubtitleLanguage() throws Exception {
		// make sure media file is defined
		checkMediaFile();
		
		Locale languageSuffix = releaseInfo.getLanguageSuffix(FileUtilities.getName(mediaFile));
		if (languageSuffix != null)
			return new Locale(languageSuffix.getISO3Language()); // force ISO3 letter-code
			
		// require subtitle file
		if (!SUBTITLE_FILES.accept(mediaFile))
			return null;
		
		return WebServices.OpenSubtitles.detectLanguage(readFile(mediaFile));
	}
	
	
	@Define("actors")
	public Object getActors() {
		return getMetaInfo().getProperty("actors");
	}
	
	
	@Define("genres")
	public Object getGenres() {
		return getMetaInfo().getProperty("genres");
	}
	
	
	@Define("director")
	public Object getDirector() {
		return getMetaInfo().getProperty("director");
	}
	
	
	@Define("certification")
	public Object getCertification() {
		return getMetaInfo().getProperty("certification");
	}
	
	
	@Define("rating")
	public Object getRating() {
		return getMetaInfo().getProperty("rating");
	}
	
	
	@Define("info")
	public synchronized AssociativeScriptObject getMetaInfo() {
		if (metaInfo == null) {
			try {
				if (infoObject instanceof Episode)
					metaInfo = WebServices.TheTVDB.getSeriesInfoByName(((Episode) infoObject).getSeriesName(), Locale.ENGLISH);
				if (infoObject instanceof Movie)
					metaInfo = WebServices.TMDb.getMovieInfo((Movie) infoObject, Locale.ENGLISH);
			} catch (Exception e) {
				throw new RuntimeException("Failed to retrieve metadata: " + infoObject, e);
			}
		}
		
		return createMapBindings(new PropertyBindings(metaInfo, null));
	}
	
	
	@Define("episodelist")
	public Object getEpisodeList() throws Exception {
		return WebServices.TheTVDB.getEpisodeList(WebServices.TheTVDB.search(getEpisode().getSeriesName()).get(0), SortOrder.Airdate, Locale.ENGLISH);
	}
	
	
	@Define("media")
	public AssociativeScriptObject getGeneralMediaInfo() {
		return createMapBindings(getMediaInfo().snapshot(StreamKind.General, 0));
	}
	
	
	@Define("video")
	public AssociativeScriptObject getVideoInfo() {
		return createMapBindings(getMediaInfo().snapshot(StreamKind.Video, 0));
	}
	
	
	@Define("audio")
	public AssociativeScriptObject getAudioInfo() {
		return createMapBindings(getMediaInfo().snapshot(StreamKind.Audio, 0));
	}
	
	
	@Define("text")
	public AssociativeScriptObject getTextInfo() {
		return createMapBindings(getMediaInfo().snapshot(StreamKind.Text, 0));
	}
	
	
	@Define("episode")
	public Episode getEpisode() {
		return (Episode) infoObject;
	}
	
	
	@Define("episodes")
	public List<Episode> getEpisodes() {
		return infoObject instanceof MultiEpisode ? ((MultiEpisode) infoObject).getEpisodes() : asList(getEpisode());
	}
	
	
	@Define("movie")
	public Movie getMovie() {
		return (Movie) infoObject;
	}
	
	
	@Define("pi")
	public Integer getPart() {
		return ((MoviePart) infoObject).getPartIndex();
	}
	
	
	@Define("pn")
	public Integer getPartCount() {
		return ((MoviePart) infoObject).getPartCount();
	}
	
	
	@Define("file")
	public File getMediaFile() {
		return mediaFile;
	}
	
	
	@Define("folder")
	public File getMediaParentFolder() {
		return mediaFile.getParentFile();
	}
	
	
	@Define("home")
	public File getUserHome() throws IOException {
		return new File(System.getProperty("user.home"));
	}
	
	
	public Object getInfoObject() {
		return infoObject;
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
			throw new RuntimeException("Invalid media file: " + mediaFile);
	}
	
	
	private synchronized MediaInfo getMediaInfo() {
		if (mediaInfo == null) {
			// make sure media file is defined
			checkMediaFile();
			
			MediaInfo newMediaInfo = new MediaInfo();
			
			// use inferred media file (e.g. actual movie file instead of subtitle file)
			if (!newMediaInfo.open(getInferredMediaFile())) {
				throw new RuntimeException("Cannot open media file: " + mediaFile);
			}
			
			mediaInfo = newMediaInfo;
		}
		
		return (MediaInfo) mediaInfo;
	}
	
	
	private String getMediaInfo(StreamKind streamKind, int streamNumber, String... keys) {
		for (String key : keys) {
			String value = getMediaInfo().get(streamKind, streamNumber, key);
			
			if (value.length() > 0)
				return value;
		}
		
		return null;
	}
	
	
	private AssociativeScriptObject createMapBindings(Map<?, ?> map) {
		return new AssociativeScriptObject(map) {
			
			@Override
			public Object getProperty(String name) {
				Object value = super.getProperty(name);
				
				if (value == null)
					throw new BindingException(name, "undefined");
				
				return value;
			}
		};
	}
	
	
	private String crc32(File file) throws IOException, InterruptedException {
		// try to get checksum from cache
		Cache cache = CacheManager.getInstance().getCache("checksum");
		Element element = cache.get(file);
		if (element != null)
			return (String) element.getValue();
		
		// compute and cache checksum
		String hash = computeHash(file, HashType.SFV);
		cache.put(new Element(file, hash));
		return hash;
	}
	
}
