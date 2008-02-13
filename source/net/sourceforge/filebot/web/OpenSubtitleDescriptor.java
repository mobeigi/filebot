
package net.sourceforge.filebot.web;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Describes a subtitle on OpenSubtitles.
 * 
 * @see OpenSubtitlesClient
 */
public class OpenSubtitleDescriptor {
	
	private Map<String, String> properties;
	
	
	public static enum Properties {
		IDSubMovieFile, MovieHash, MovieByteSize, MovieTimeMS, MovieFrames, IDSubtitleFile, SubFileName, SubActualCD, SubSize, SubHash, IDSubtitle, UserID, SubLanguageID, SubFormat, SubSumCD, SubAuthorComment, SubAddDate, SubBad, SubRating, SubDownloadsCnt, MovieReleaseName, IDMovie, IDMovieImdb, MovieName, MovieNameEng, MovieYear, MovieImdbRating, UserNickName, ISO639, LanguageName, SubDownloadLink, ZipDownloadLink,
	}
	
	
	public OpenSubtitleDescriptor(Map<String, String> properties) {
		this.properties = properties;
	}
	

	public Map<String, String> getPropertyMap() {
		return properties;
	}
	

	public String getProperty(Properties property) {
		return properties.get(property.name());
	}
	

	public long getSize() {
		return Long.parseLong(getProperty(Properties.SubSize));
	}
	

	public URL getDownloadLink() {
		String link = getProperty(Properties.SubDownloadLink);
		
		try {
			return new URL(link);
		} catch (MalformedURLException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid download link: " + link, e);
			return null;
		}
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getProperty(Properties.SubFileName), getProperty(Properties.LanguageName));
	}
	
}
