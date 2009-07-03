
package net.sourceforge.filebot.web;


import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import net.sourceforge.tuned.ByteBufferOutputStream;
import net.sourceforge.tuned.FileUtilities;


/**
 * Describes a subtitle on OpenSubtitles.
 * 
 * @see OpenSubtitlesXmlRpc
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
	

	public String getProperty(Property key) {
		return properties.get(key);
	}
	

	@Override
	public String getName() {
		return FileUtilities.getNameWithoutExtension(getProperty(Property.SubFileName));
	}
	

	@Override
	public String getLanguageName() {
		return getProperty(Property.LanguageName);
	}
	

	@Override
	public String getType() {
		return getProperty(Property.SubFormat);
	}
	

	public int getSize() {
		return Integer.parseInt(getProperty(Property.SubSize));
	}
	

	@Override
	public ByteBuffer fetch() throws Exception {
		URL resource = new URL(getProperty(Property.SubDownloadLink));
		InputStream stream = new GZIPInputStream(resource.openStream());
		
		try {
			ByteBufferOutputStream buffer = new ByteBufferOutputStream(getSize());
			
			// read all
			buffer.transferFully(stream);
			
			return buffer.getByteBuffer();
		} finally {
			stream.close();
		}
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
