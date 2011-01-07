
package net.sourceforge.filebot;


import static net.sourceforge.filebot.Settings.*;

import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.IMDbClient;
import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.SublightSubtitleClient;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.TVDotComClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TheTVDBClient;
import net.sourceforge.filebot.web.VideoHashSubtitleService;


/**
 * Reuse the same web service client so login, cache, etc. can be shared.
 */
public final class WebServices {
	
	// episode dbs
	public static final TVRageClient TVRage = new TVRageClient();
	public static final AnidbClient AniDB = new AnidbClient("filebot", 1);
	public static final TVDotComClient TVDotCom = new TVDotComClient();
	public static final IMDbClient IMDb = new IMDbClient();
	public static final TheTVDBClient TheTVDB = new TheTVDBClient(getApplicationProperty("thetvdb.apikey"));
	
	// subtitle dbs
	public static final OpenSubtitlesClient OpenSubtitles = new OpenSubtitlesClient(String.format("%s %s", getApplicationName(), getApplicationVersion()));
	public static final SublightSubtitleClient Sublight = new SublightSubtitleClient(getApplicationName(), getApplicationProperty("sublight.apikey"));
	public static final SubsceneSubtitleClient Subscene = new SubsceneSubtitleClient();
	

	public static EpisodeListProvider[] getEpisodeListProviders() {
		return new EpisodeListProvider[] { TVRage, AniDB, TVDotCom, IMDb, TheTVDB };
	}
	

	public static SubtitleProvider[] getSubtitleProviders() {
		return new SubtitleProvider[] { OpenSubtitles, Subscene, Sublight };
	}
	

	public static VideoHashSubtitleService[] getVideoHashSubtitleServices() {
		return new VideoHashSubtitleService[] { OpenSubtitles, Sublight };
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private WebServices() {
		throw new UnsupportedOperationException();
	}
	
}
