
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.Settings.*;

import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.SublightSubtitleClient;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.SubtitleSourceClient;
import net.sourceforge.filebot.web.VideoHashSubtitleService;


final class SubtitleServices {
	
	public static final OpenSubtitlesClient openSubtitlesClient = new OpenSubtitlesClient(String.format("%s %s", getApplicationName(), getApplicationVersion()));
	public static final SublightSubtitleClient sublightSubtitleClient = new SublightSubtitleClient(getApplicationName(), getApplicationProperty("sublight.apikey"));
	
	public static final SubsceneSubtitleClient subsceneSubtitleClient = new SubsceneSubtitleClient();
	public static final SubtitleSourceClient subtitleSourceClient = new SubtitleSourceClient();
	

	public static SubtitleProvider[] getSubtitleProviders() {
		return new SubtitleProvider[] { openSubtitlesClient, subsceneSubtitleClient, sublightSubtitleClient, subtitleSourceClient };
	}
	

	public static VideoHashSubtitleService[] getVideoHashSubtitleServices() {
		return new VideoHashSubtitleService[] { openSubtitlesClient, sublightSubtitleClient };
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private SubtitleServices() {
		throw new UnsupportedOperationException();
	}
	
}
