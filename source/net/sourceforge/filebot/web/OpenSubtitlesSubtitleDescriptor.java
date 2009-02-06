
package net.sourceforge.filebot.web;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.sourceforge.tuned.DownloadTask;


/**
 * Describes a subtitle on OpenSubtitles.
 * 
 * @see OpenSubtitlesClient
 */
public class OpenSubtitlesSubtitleDescriptor implements SubtitleDescriptor {
	
	private final Map<Property, String> properties;
	
	
	public static enum Property {
		IDSubMovieFile,
		MovieHash,
		MovieByteSize,
		MovieTimeMS,
		MovieFrames,
		IDSubtitleFile,
		SubFileName,
		SubActualCD,
		SubSize,
		SubHash,
		IDSubtitle,
		UserID,
		SubLanguageID,
		SubFormat,
		SubSumCD,
		SubAuthorComment,
		SubAddDate,
		SubBad,
		SubRating,
		SubDownloadsCnt,
		MovieReleaseName,
		IDMovie,
		IDMovieImdb,
		MovieName,
		MovieNameEng,
		MovieYear,
		MovieImdbRating,
		UserNickName,
		ISO639,
		LanguageName,
		SubDownloadLink,
		ZipDownloadLink;
		
		public static <V> EnumMap<Property, V> asEnumMap(Map<String, V> stringMap) {
			EnumMap<Property, V> enumMap = new EnumMap<Property, V>(Property.class);
			
			for (Entry<String, V> entry : stringMap.entrySet()) {
				try {
					enumMap.put(Property.valueOf(entry.getKey()), entry.getValue());
				} catch (IllegalArgumentException e) {
					// illegal enum constant, just ignore
				}
			}
			
			return enumMap;
		}
	}
	
	
	public OpenSubtitlesSubtitleDescriptor(Map<Property, String> properties) {
		this.properties = new EnumMap<Property, String>(properties);
	}
	

	public Map<Property, String> getProperties() {
		return Collections.unmodifiableMap(properties);
	}
	

	@Override
	public String getName() {
		return properties.get(Property.SubFileName);
	}
	

	@Override
	public String getLanguageName() {
		return properties.get(Property.LanguageName);
	}
	

	@Override
	public DownloadTask createDownloadTask() {
		try {
			return new DownloadTask(new URL(properties.get(Property.ZipDownloadLink)));
		} catch (MalformedURLException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	

	@Override
	public String getArchiveType() {
		return "zip";
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
