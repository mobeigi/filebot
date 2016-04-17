package net.filebot.format;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.format.Define.*;
import static net.filebot.format.ExpressionFormatMethods.*;
import static net.filebot.hash.VerificationUtilities.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.media.XattrMetaInfo.*;
import static net.filebot.similarity.Normalization.*;
import static net.filebot.subtitle.SubtitleUtilities.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.RegularExpressions.*;
import static net.filebot.util.StringUtilities.*;
import static net.filebot.web.EpisodeFormat.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.cedarsoftware.util.io.JsonWriter;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.Language;
import net.filebot.MediaTypes;
import net.filebot.MetaAttributeView;
import net.filebot.Settings;
import net.filebot.Settings.ApplicationFolder;
import net.filebot.WebServices;
import net.filebot.hash.HashType;
import net.filebot.media.NamingStandard;
import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;
import net.filebot.mediainfo.MediaInfoException;
import net.filebot.similarity.SimilarityComparator;
import net.filebot.util.FileUtilities;
import net.filebot.util.WeakValueHashMap;
import net.filebot.web.AudioTrack;
import net.filebot.web.Episode;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.Movie;
import net.filebot.web.MoviePart;
import net.filebot.web.MultiEpisode;
import net.filebot.web.SeriesInfo;
import net.filebot.web.SimpleDate;
import net.filebot.web.SortOrder;
import net.filebot.web.TMDbClient.MovieInfo;
import net.filebot.web.TheTVDBSeriesInfo;

public class MediaBindingBean {

	private final Object infoObject;
	private final File mediaFile;
	private final Map<File, ?> context;

	private MediaInfo mediaInfo;
	private Object metaInfo;

	public MediaBindingBean(Object infoObject, File mediaFile) {
		this(infoObject, mediaFile, singletonMap(mediaFile, infoObject));
	}

	public MediaBindingBean(Object infoObject, File mediaFile, Map<File, ?> context) {
		this.infoObject = infoObject;
		this.mediaFile = mediaFile;
		this.context = context;
	}

	public Object getInfoObject() {
		return infoObject;
	}

	public File getFileObject() {
		return mediaFile;
	}

	@Define(undefined)
	public <T> T undefined(String name) {
		// omit expressions that depend on undefined values
		throw new BindingException(name, EXCEPTION_UNDEFINED);
	}

	@Define("n")
	public String getName() {
		if (infoObject instanceof Episode)
			return getEpisode().getSeriesName();
		else if (infoObject instanceof Movie)
			return getMovie().getName();
		else if (infoObject instanceof AudioTrack && getAlbumArtist() != null)
			return getAlbumArtist();
		else if (infoObject instanceof AudioTrack)
			return getArtist();
		else if (infoObject instanceof File)
			return FileUtilities.getName((File) infoObject);

		return null;
	}

	@Define("y")
	public Integer getYear() {
		if (infoObject instanceof Episode)
			return getEpisode().getSeriesInfo().getStartDate().getYear();
		if (infoObject instanceof Movie)
			return getMovie().getYear();
		if (infoObject instanceof AudioTrack)
			return getReleaseDate().getYear();

		return null;
	}

	@Define("ny")
	public String getNameWithYear() {
		String n = getName().toString();
		String y = " (" + getYear().toString() + ")";
		return n.endsWith(y) ? n : n + y; // account for TV Shows that contain the year in the series name, e.g. Doctor Who (2005)
	}

	@Define("s")
	public Integer getSeasonNumber() {
		return getEpisode().getSeason();
	}

	@Define("e")
	public Integer getEpisodeNumber() {
		return getEpisode().getEpisode();
	}

	@Define("es")
	public List<Integer> getEpisodeNumbers() {
		List<Integer> n = new ArrayList<Integer>();
		for (Episode it : getEpisodes()) {
			n.add(it.getEpisode() == null ? it.getSpecial() == null ? -1 : it.getSpecial() : it.getEpisode());
		}
		return n;
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
		if (infoObject instanceof AudioTrack) {
			return getMusic().getTrackTitle() != null ? getMusic().getTrackTitle() : getMusic().getTitle();
		}

		// enforce title length limit by default
		return truncateText(infoObject instanceof MultiEpisode ? SeasonEpisode.formatMultiTitle(getEpisodes()) : getEpisode().getTitle(), 150);
	}

	@Define("d")
	public SimpleDate getReleaseDate() {
		if (infoObject instanceof Episode) {
			return getEpisode().getAirdate();
		}
		if (infoObject instanceof Movie) {
			return (SimpleDate) getMetaInfo().getProperty("released");
		}
		if (infoObject instanceof AudioTrack) {
			return getMusic().getAlbumReleaseDate();
		}
		if (infoObject instanceof File) {
			return new SimpleDate(getCreationDate(((File) infoObject)));
		}

		// no date info for the model
		return null;
	}

	@Define("airdate")
	public SimpleDate airdate() {
		return getEpisode().getAirdate();
	}

	@Define("age")
	public Number getAgeInDays() {
		SimpleDate releaseDate = getReleaseDate();
		if (releaseDate != null) {
			long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - releaseDate.getTimeStamp());
			if (days >= 0) {
				return days;
			}
		}
		return null;
	}

	@Define("startdate")
	public SimpleDate startdate() {
		return getEpisode().getSeriesInfo().getStartDate();
	}

	@Define("absolute")
	public Integer getAbsoluteEpisodeNumber() {
		return getEpisode().getAbsolute();
	}

	@Define("special")
	public Integer getSpecialNumber() {
		return getEpisode().getSpecial();
	}

	@Define("series")
	public SeriesInfo getSeriesInfo() {
		return getEpisode().getSeriesInfo();
	}

	@Define("alias")
	public List<String> getAliasNames() {
		if (infoObject instanceof Movie) {
			return asList(getMovie().getAliasNames());
		}
		if (infoObject instanceof Episode) {
			return getSeriesInfo().getAliasNames();
		}
		return emptyList();
	}

	@Define("primaryTitle")
	public String getPrimaryTitle() throws Exception {
		if (infoObject instanceof Movie) {
			return WebServices.TheMovieDB.getMovieInfo(getMovie(), Locale.ENGLISH, false).getOriginalName();
		}

		if (infoObject instanceof Episode) {
			// force English series name for TheTVDB data
			if (WebServices.TheTVDB.getIdentifier().equals(getSeriesInfo().getDatabase())) {
				return WebServices.TheTVDB.getSeriesInfo(getSeriesInfo().getId(), Locale.ENGLISH).getName();
			}

			// default to series info name (for anime this would be the primary title)
			return getSeriesInfo().getName();
		}

		return null;
	}

	@Define("tmdbid")
	public String getTmdbId() throws Exception {
		int tmdbid = getMovie().getTmdbId();

		if (tmdbid <= 0) {
			if (getMovie().getImdbId() <= 0) {
				return null;
			}

			// lookup IMDbID for TMDbID
			try {
				tmdbid = WebServices.TheMovieDB.getMovieInfo(getMovie(), Locale.ENGLISH, false).getId();
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		return String.valueOf(tmdbid);
	}

	@Define("imdbid")
	public String getImdbId() throws Exception {
		Integer imdbid = getMovie().getImdbId();

		if (imdbid <= 0) {
			if (getMovie().getTmdbId() <= 0) {
				return null;
			}

			// lookup IMDbID for TMDbID
			try {
				imdbid = WebServices.TheMovieDB.getMovieInfo(getMovie(), Locale.ENGLISH, false).getImdbId();
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		return imdbid != null ? String.format("tt%07d", imdbid) : null;
	}

	@Define("vc")
	public String getVideoCodec() {
		// e.g. XviD, x264, DivX 5, MPEG-4 Visual, AVC, etc.
		String codec = getMediaInfo(StreamKind.Video, 0, "Encoded_Library_Name", "Encoded_Library/Name", "CodecID/Hint", "Format");

		// get first token (e.g. DivX 5 => DivX)
		return SPACE.splitAsStream(codec).findFirst().get();
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
		return SPACE.splitAsStream(extensions).findFirst().get().toLowerCase();
	}

	@Define("vf")
	public String getVideoFormat() {
		int width = Integer.parseInt(getMediaInfo(StreamKind.Video, 0, "Width"));
		int height = Integer.parseInt(getMediaInfo(StreamKind.Video, 0, "Height"));

		int ns = 0;
		int[] ws = new int[] { 15360, 7680, 3840, 1920, 1280, 1024, 854, 852, 720, 688, 512, 320 };
		int[] hs = new int[] { 8640, 4320, 2160, 1080, 720, 576, 576, 480, 480, 360, 240, 240 };
		for (int i = 0; i < ws.length - 1; i++) {
			if ((width >= ws[i] || height >= hs[i]) || (width > ws[i + 1] && height > hs[i + 1])) {
				ns = hs[i];
				break;
			}
		}
		if (ns > 0) {
			// e.g. 720p, nobody actually wants files to be tagged as interlaced, e.g. 720i
			return String.format("%dp", ns);
		}
		return null; // video too small
	}

	@Define("hpi")
	public String getExactVideoFormat() {
		String height = getMediaInfo(StreamKind.Video, 0, "Height");
		String scanType = getMediaInfo(StreamKind.Video, 0, "ScanType");

		// e.g. 720p
		return height + Character.toLowerCase(scanType.charAt(0));
	}

	@Define("af")
	public String getAudioChannels() {
		String channels = getMediaInfo(StreamKind.Audio, 0, "Channel(s)_Original", "Channel(s)");

		// get first number, e.g. 6ch
		return SPACE.splitAsStream(channels).findFirst().get() + "ch";
	}

	@Define("channels")
	public String getAudioChannelPositions() {
		String channels = getMediaInfo(StreamKind.Audio, 0, "ChannelPositions/String2", "Channel(s)_Original", "Channel(s)");

		// e.g. ChannelPositions/String2: 3/2/2.1 / 3/2/0.1 (one audio stream may contain multiple multi-channel streams)
		double d = SPACE.splitAsStream(channels).mapToDouble(s -> {
			return SLASH.splitAsStream(s).mapToDouble(Double::parseDouble).reduce(0, (a, b) -> a + b);
		}).filter(it -> it > 0).max().getAsDouble();

		return BigDecimal.valueOf(d).setScale(1, RoundingMode.HALF_UP).toPlainString();
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
		return (float) dim.get(0) / dim.get(1) > 1.37f ? "WS" : null;
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

		return asList(Integer.parseInt(width), Integer.parseInt(height));
	}

	@Define("original")
	public String getOriginalFileName() {
		return getOriginalFileName(getMediaFile());
	}

	@Define("xattr")
	public Object getMetaAttributesObject() throws Exception {
		return xattr.getMetaInfo(getMediaFile());
	}

	@Define("crc32")
	public String getCRC32() throws Exception {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();

		// try to get checksum from file name
		for (String filename : new String[] { getOriginalFileName(inferredMediaFile), inferredMediaFile.getName() }) {
			if (filename != null) {
				String checksum = getEmbeddedChecksum(filename);
				if (checksum != null) {
					return checksum;
				}
			}
		}

		// try to get checksum from sfv file
		String checksum = getHashFromVerificationFile(inferredMediaFile, HashType.SFV, 3);
		if (checksum != null) {
			return checksum;
		}

		// try CRC32 xattr (as stored by verify script)
		try {
			MetaAttributeView xattr = new MetaAttributeView(inferredMediaFile);
			checksum = xattr.get("CRC32");
			if (checksum != null) {
				return checksum;
			}
		} catch (Exception e) {
			// ignore if xattr metadata is not supported for the given file
		}

		// calculate checksum from file
		Cache cache = Cache.getCache("crc32", CacheType.Ephemeral);
		return (String) cache.computeIfAbsent(inferredMediaFile.getCanonicalPath(), it -> crc32(inferredMediaFile));
	}

	@Define("fn")
	public String getFileName() {
		// name without file extension
		return FileUtilities.getName(getMediaFile());
	}

	@Define("ext")
	public String getExtension() {
		// file extension
		return FileUtilities.getExtension(getMediaFile());
	}

	@Define("source")
	public String getVideoSource() {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();

		// look for video source patterns in media file and it's parent folder
		return releaseInfo.getVideoSource(inferredMediaFile.getParent(), inferredMediaFile.getName(), getOriginalFileName(inferredMediaFile));
	}

	@Define("tags")
	public List<String> getVideoTags() {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();

		// look for video source patterns in media file and it's parent folder
		List<String> matches = releaseInfo.getVideoTags(inferredMediaFile.getParent(), inferredMediaFile.getName(), getOriginalFileName(inferredMediaFile));
		if (matches.isEmpty()) {
			return null;
		}

		Set<String> tags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		for (String m : matches) {
			// heavy normalization of whatever pattern was matched with the regex pattern
			tags.add(lowerTrail(upperInitial(normalizePunctuation(normalizeSpace(m, " ")))));
		}
		return new ArrayList<String>(tags);

	}

	@Define("s3d")
	public String getStereoscopic3D() {
		return releaseInfo.getStereoscopic3D(getFileName());
	}

	@Define("group")
	public String getReleaseGroup() throws Exception {
		// use inferred media file
		File inferredMediaFile = getInferredMediaFile();

		// consider foldername, filename and original filename
		String[] filenames = new String[] { inferredMediaFile.getParentFile().getName(), getNameWithoutExtension(inferredMediaFile.getName()), getOriginalFileName(inferredMediaFile) };

		// reduce false positives by removing the know titles from the name
		Pattern nonGroupPattern = releaseInfo.getCustomRemovePattern(getKeywords());
		for (int i = 0; i < filenames.length; i++) {
			if (filenames[i] == null)
				continue;

			filenames[i] = releaseInfo.clean(filenames[i], nonGroupPattern, releaseInfo.getVideoSourcePattern(), releaseInfo.getVideoFormatPattern(true), releaseInfo.getResolutionPattern(), releaseInfo.getStructureRootPattern());
		}

		// look for release group names in media file and it's parent folder
		return releaseInfo.getReleaseGroup(filenames);
	}

	@Define("subt")
	public String getSubtitleTags() throws Exception {
		if (!SUBTITLE_FILES.accept(getMediaFile())) {
			return null;
		}

		Language language = getLanguageTag();
		if (language != null) {
			String tag = '.' + language.getISO3B();
			String category = releaseInfo.getSubtitleCategoryTag(FileUtilities.getName(getMediaFile()), getOriginalFileName(getMediaFile()));
			if (category != null) {
				return tag + '.' + category;
			}
			return tag;
		}

		return null;
	}

	@Define("lang")
	public Language getLanguageTag() throws Exception {
		Locale languageSuffix = releaseInfo.getSubtitleLanguageTag(FileUtilities.getName(getMediaFile()), getOriginalFileName(getMediaFile()));
		if (languageSuffix != null) {
			return Language.getLanguage(languageSuffix);
		}

		// try to auto-detect subtitle language
		if (SUBTITLE_FILES.accept(getMediaFile())) {
			try {
				return Language.getLanguage(detectSubtitleLanguage(getMediaFile()));
			} catch (Throwable e) {
				throw new RuntimeException("Failed to auto-detect subtitle language: " + e, e);
			}
		}

		return null;
	}

	@Define("languages")
	public Object getSpokenLanguages() {
		List<?> languages = infoObject instanceof Movie ? (List<?>) getMetaInfo().getProperty("spokenLanguages") : singletonList(getMetaInfo().getProperty("language"));
		return languages.stream().map(it -> new Locale(it.toString()).getDisplayLanguage(Locale.ENGLISH)).collect(toList());
	}

	@Define("actors")
	public Object getActors() {
		return getMetaInfo().getProperty("actors");
	}

	@Define("genres")
	public Object getGenres() {
		return getMetaInfo().getProperty("genres");
	}

	@Define("genre")
	public String getPrimaryGenre() {
		return ((Iterable<?>) getGenres()).iterator().next().toString();
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

	@Define("collection")
	public Object getCollection() {
		return getMetaInfo().getProperty("collection");
	}

	@Define("info")
	public synchronized AssociativeScriptObject getMetaInfo() {
		if (metaInfo == null) {
			try {
				if (infoObject instanceof Episode) {
					metaInfo = getSeriesInfo();
				} else if (infoObject instanceof Movie) {
					if (getMovie().getTmdbId() > 0) {
						metaInfo = WebServices.TheMovieDB.getMovieInfo(getMovie(), getMovie().getLanguage() == null ? Locale.ENGLISH : getMovie().getLanguage(), true);
					} else if (getMovie().getImdbId() > 0) {
						metaInfo = WebServices.OMDb.getMovieInfo(getMovie());
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to retrieve extended metadata: " + infoObject, e);
			}
		}

		if (metaInfo == null) {
			throw new UnsupportedOperationException("Extended metadata not available");
		}

		return createPropertyBindings(metaInfo);
	}

	@Define("omdb")
	public synchronized AssociativeScriptObject getOmdbApiInfo() {
		Object metaInfo = null;

		try {
			if (infoObject instanceof Episode) {
				if (WebServices.TheTVDB.getIdentifier().equals(getSeriesInfo().getDatabase())) {
					TheTVDBSeriesInfo extendedSeriesInfo = (TheTVDBSeriesInfo) WebServices.TheTVDB.getSeriesInfo(getSeriesInfo().getId(), Locale.ENGLISH);
					if (extendedSeriesInfo.getImdbId() != null) {
						metaInfo = WebServices.OMDb.getMovieInfo(new Movie(grepImdbId(extendedSeriesInfo.getImdbId()).iterator().next()));
					}
				}
			}
			if (infoObject instanceof Movie) {
				if (getMovie().getTmdbId() > 0) {
					MovieInfo movieInfo = WebServices.TheMovieDB.getMovieInfo(getMovie(), Locale.ENGLISH, false);
					if (movieInfo.getImdbId() != null) {
						metaInfo = WebServices.OMDb.getMovieInfo(new Movie(movieInfo.getImdbId()));
					}
				} else if (getMovie().getImdbId() > 0) {
					metaInfo = WebServices.OMDb.getMovieInfo(getMovie());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve extended metadata: " + infoObject, e);
		}

		if (metaInfo == null) {
			throw new UnsupportedOperationException("Extended metadata not available");
		}

		return createPropertyBindings(metaInfo);
	}

	@Define("az")
	public String getSortInitial() {
		try {
			return sortInitial(getCollection().toString());
		} catch (Exception e) {
			return sortInitial(getName());
		}
	}

	@Define("episodelist")
	public Object getEpisodeList() throws Exception {
		return WebServices.getEpisodeListProvider(getSeriesInfo().getDatabase()).getEpisodeList(getSeriesInfo().getId(), SortOrder.forName(getSeriesInfo().getOrder()), new Locale(getSeriesInfo().getLanguage()));
	}

	@Define("localize")
	public Object getLocalizedInfoObject() throws Exception {
		return new DynamicBindings(key -> {
			Language language = Language.findLanguage(key);
			if (language != null) {
				Object localizedInfo = null;
				try {
					if (infoObject instanceof Movie) {
						localizedInfo = WebServices.TheMovieDB.getMovieInfo(getMovie(), language.getLocale(), true);
					} else if (infoObject instanceof Episode) {
						EpisodeListProvider db = WebServices.getEpisodeListProvider(getSeriesInfo().getDatabase());
						List<Episode> episodes = db.getEpisodeList(getSeriesInfo().getId(), SortOrder.forName(getSeriesInfo().getOrder()), language.getLocale());
						localizedInfo = episodes.stream().filter(it -> getEpisode().getNumbers().equals(it.getNumbers())).findFirst().orElse(null);
					}
				} catch (Exception e) {
					throw new BindingException(key, e);
				}

				if (localizedInfo != null) {
					return createPropertyBindings(localizedInfo);
				}
			}
			return undefined(key);
		}, Language.availableLanguages().stream().map(Language::getName));
	}

	@Define("bitrate")
	public Long getBitRate() {
		return new Double(getMediaInfo(StreamKind.General, 0, "OverallBitRate")).longValue();
	}

	@Define("duration")
	public Long getDuration() {
		return (long) Double.parseDouble(getMediaInfo(StreamKind.General, 0, "Duration"));
	}

	@Define("seconds")
	public Integer getSeconds() {
		return (int) (getDuration() / 1000);
	}

	@Define("minutes")
	public Integer getDurationInMinutes() {
		return (int) (getDuration() / 60000);
	}

	@Define("media")
	public AssociativeScriptObject getGeneralMediaInfo() {
		return createMediaInfoBindings(StreamKind.General).get(0);
	}

	@Define("menu")
	public AssociativeScriptObject getMenuInfo() {
		return createMediaInfoBindings(StreamKind.Menu).get(0);
	}

	@Define("image")
	public AssociativeScriptObject getImageInfo() {
		return createMediaInfoBindings(StreamKind.Image).get(0);
	}

	@Define("video")
	public List<AssociativeScriptObject> getVideoInfoList() {
		return createMediaInfoBindings(StreamKind.Video);
	}

	@Define("audio")
	public List<AssociativeScriptObject> getAudioInfoList() {
		return createMediaInfoBindings(StreamKind.Audio);
	}

	@Define("text")
	public List<AssociativeScriptObject> getTextInfoList() {
		return createMediaInfoBindings(StreamKind.Text);
	}

	@Define("chapters")
	public List<AssociativeScriptObject> getChaptersInfoList() {
		return createMediaInfoBindings(StreamKind.Chapters);
	}

	@Define("artist")
	public String getArtist() {
		return getMusic().getArtist();
	}

	@Define("albumArtist")
	public String getAlbumArtist() {
		return getMusic().getAlbumArtist();
	}

	@Define("album")
	public String getAlbum() {
		return getMusic().getAlbum();
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

	@Define("music")
	public AudioTrack getMusic() {
		return (AudioTrack) infoObject;
	}

	@Define("pi")
	public Integer getPart() {
		if (infoObject instanceof AudioTrack)
			return getMusic().getTrack();
		if (infoObject instanceof MoviePart)
			return ((MoviePart) infoObject).getPartIndex();

		return null;
	}

	@Define("pn")
	public Integer getPartCount() {
		if (infoObject instanceof AudioTrack)
			return getMusic().getTrackCount();
		if (infoObject instanceof MoviePart)
			return ((MoviePart) infoObject).getPartCount();

		return null;
	}

	@Define("mediaType")
	public List<String> getMediaType() throws Exception {
		// format engine does not allow / in binding value
		return SLASH.splitAsStream(MediaTypes.getDefault().getMediaType(getExtension())).collect(toList());
	}

	@Define("file")
	public File getMediaFile() {
		// make sure file is not null, and that it is an existing file
		if (mediaFile == null) {
			throw new IllegalStateException(EXCEPTION_SAMPLE_FILE_NOT_SET);
		}

		return mediaFile;
	}

	@Define("folder")
	public File getMediaParentFolder() {
		return getMediaFile().getParentFile();
	}

	@Define("bytes")
	public Long getFileSize() {
		return getInferredMediaFile().length();
	}

	@Define("megabytes")
	public String getFileSizeInMegaBytes() {
		return String.format("%.0f", getFileSize() / Math.pow(1000, 2));
	}

	@Define("gigabytes")
	public String getFileSizeInGigaBytes() {
		return String.format("%.1f", getFileSize() / Math.pow(1000, 3));
	}

	@Define("home")
	public File getUserHome() {
		return ApplicationFolder.UserHome.getCanonicalFile();
	}

	@Define("now")
	public Date getNow() {
		return new Date();
	}

	@Define("output")
	public File getUserDefinedOutputFolder() throws IOException {
		return new File(Settings.getApplicationArguments().output).getCanonicalFile();
	}

	@Define("defines")
	public Map<String, String> getUserDefinedArguments() throws IOException {
		return Settings.getApplicationArguments().defines;
	}

	@Define("label")
	public String getUserDefinedLabel() throws IOException {
		for (Entry<String, String> it : getUserDefinedArguments().entrySet()) {
			if (it.getKey().endsWith("label")) {
				if (it.getValue() != null && it.getValue().length() > 0) {
					return it.getValue();
				}
			}
		}
		return null;
	}

	@Define("i")
	public Integer getModelIndex() {
		return 1 + identityIndexOf(context.values(), getInfoObject());
	}

	@Define("di")
	public Integer getDuplicateIndex() {
		List<Object> duplicates = new ArrayList<Object>();
		for (Object it : context.values()) {
			if (getInfoObject().equals(it)) {
				duplicates.add(it);
			}
		}
		return 1 + identityIndexOf(duplicates, getInfoObject());
	}

	@Define("plex")
	public File getPlexStandardPath() throws Exception {
		String path = NamingStandard.Plex.getPath(infoObject);
		try {
			path = path.concat(getSubtitleTags());
		} catch (Exception e) {
			// ignore => no language tags
		}
		return new File(path);
	}

	@Define("self")
	public AssociativeScriptObject getSelf() {
		return createBindingObject(mediaFile, infoObject, context);
	}

	@Define("model")
	public List<AssociativeScriptObject> getModel() {
		List<AssociativeScriptObject> result = new ArrayList<AssociativeScriptObject>();
		for (Entry<File, ?> it : context.entrySet()) {
			result.add(createBindingObject(it.getKey(), it.getValue(), context));
		}
		return result;
	}

	@Define("json")
	public String getInfoObjectDump() {
		return JsonWriter.objectToJson(infoObject);
	}

	public File getInferredMediaFile() {
		if (getMediaFile().isDirectory()) {
			// just select the first video file in the folder as media sample
			SortedSet<File> videos = new TreeSet<File>(filter(listFiles(getMediaFile()), VIDEO_FILES));
			if (videos.size() > 0) {
				return videos.iterator().next();
			}
		} else if (SUBTITLE_FILES.accept(getMediaFile()) || ((infoObject instanceof Episode || infoObject instanceof Movie) && !VIDEO_FILES.accept(getMediaFile()))) {
			// prefer equal match from current context if possible
			if (context != null) {
				for (Entry<File, ?> it : context.entrySet()) {
					if (infoObject.equals(it.getValue()) && VIDEO_FILES.accept(it.getKey())) {
						return it.getKey();
					}
				}
			}

			// file is a subtitle, or nfo, etc
			String baseName = stripReleaseInfo(FileUtilities.getName(getMediaFile())).toLowerCase();
			List<File> videos = getChildren(getMediaFile().getParentFile(), VIDEO_FILES);

			// find corresponding movie file
			for (File movieFile : videos) {
				if (!baseName.isEmpty() && stripReleaseInfo(FileUtilities.getName(movieFile)).toLowerCase().startsWith(baseName)) {
					return movieFile;
				}
			}

			// still no good match found -> just take the most probable video from the same folder
			if (videos.size() > 0) {
				sort(videos, SimilarityComparator.compareTo(FileUtilities.getName(getMediaFile()), FileUtilities::getName));
				return videos.get(0);
			}
		}

		return getMediaFile();
	}

	private static final Map<File, MediaInfo> sharedMediaInfoObjects = new WeakValueHashMap<File, MediaInfo>(64);

	private synchronized MediaInfo getMediaInfo() {
		// lazy initialize
		if (mediaInfo == null) {
			// use inferred media file (e.g. actual movie file instead of subtitle file)
			File inferredMediaFile = getInferredMediaFile();

			synchronized (sharedMediaInfoObjects) {
				mediaInfo = sharedMediaInfoObjects.computeIfAbsent(inferredMediaFile, f -> {
					try {
						return new MediaInfo().open(f);
					} catch (Exception e) {
						throw new MediaInfoException(e.getMessage());
					}
				});
			}
		}

		return mediaInfo;
	}

	private Integer identityIndexOf(Iterable<?> c, Object o) {
		Iterator<?> itr = c.iterator();
		for (int i = 0; itr.hasNext(); i++) {
			Object next = itr.next();
			if (o == next)
				return i;
		}
		return null;
	}

	private String getMediaInfo(StreamKind streamKind, int streamNumber, String... keys) {
		for (String key : keys) {
			String value = getMediaInfo().get(streamKind, streamNumber, key);
			if (value.length() > 0) {
				return value;
			}
		}
		return undefined(String.format("%s[%d][%s]", streamKind, streamNumber, join(keys, ", ")));
	}

	private AssociativeScriptObject createBindingObject(File file, Object info, Map<File, ?> context) {
		MediaBindingBean mediaBindingBean = new MediaBindingBean(info, file, context) {

			@Override
			@Define(undefined)
			public <T> T undefined(String name) {
				return null; // never throw exceptions for empty or null values
			}
		};
		return new AssociativeScriptObject(new ExpressionBindings(mediaBindingBean));
	}

	private AssociativeScriptObject createPropertyBindings(Object object) {
		return new AssociativeScriptObject(new PropertyBindings(object, null)) {

			@Override
			public Object getProperty(String name) {
				Object value = super.getProperty(name);
				if (value == null) {
					return undefined(name);
				}
				if (value instanceof CharSequence) {
					return replacePathSeparators(value.toString()).trim(); // auto-clean value of path separators
				}
				return value;
			}
		};
	}

	private List<AssociativeScriptObject> createMediaInfoBindings(StreamKind kind) {
		return getMediaInfo().snapshot().get(kind).stream().map(AssociativeScriptObject::new).collect(toList());
	}

	private String getOriginalFileName(File file) {
		String name = xattr.getOriginalName(file);
		return name == null ? null : getNameWithoutExtension(name);
	}

	private List<String> getKeywords() {
		// collect key information
		Set<Object> keys = new HashSet<Object>();
		keys.add(getName());
		keys.add(getYear());
		keys.addAll(getAliasNames());

		if (infoObject instanceof Episode) {
			for (Episode it : getEpisodes()) {
				keys.addAll(it.getSeriesNames());
				keys.add(it.getTitle());
			}
		}

		// word list for exclude pattern
		List<String> words = new ArrayList<String>(keys.size());
		for (Object it : keys) {
			String w = normalizePunctuation(normalizeSpace(Objects.toString(it, ""), " "));
			if (w != null && w.length() > 0) {
				words.add(w);
			}
		}
		return words;
	}

	@Override
	public String toString() {
		return String.format("%s ⇔ %s", infoObject, mediaFile == null ? null : mediaFile.getName());
	}

	public static final String EXCEPTION_UNDEFINED = "undefined";
	public static final String EXCEPTION_SAMPLE_FILE_NOT_SET = "Sample file has not been set. Click \"Change Sample\" to select a sample file.";

}
