
package net.sourceforge.filebot.web;


import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.ImageIcon;


public abstract class EpisodeListClient {
	
	private static LinkedHashSet<EpisodeListClient> registry = new LinkedHashSet<EpisodeListClient>();
	
	static {
		registry.add(new TvdotcomClient());
		registry.add(new AnidbClient());
		registry.add(new TVRageClient());
	}
	
	
	public static Iterable<EpisodeListClient> getAvailableEpisodeListClients() {
		return registry;
	}
	

	public static EpisodeListClient forName(String name) {
		for (EpisodeListClient client : registry) {
			if (name.equals(client.getName()))
				return client;
		}
		
		return null;
	}
	

	/**
	 * List of shows
	 */
	public abstract List<String> search(String searchterm) throws Exception;
	

	/**
	 * @param showname
	 * @param season number of season, 0 for all seasons
	 * @return
	 * @throws Exception
	 */
	public abstract List<Episode> getEpisodeList(String showname, int season) throws Exception;
	

	public abstract URL getEpisodeListUrl(String showname, int season);
	

	public boolean isSingleSeasonSupported() {
		return singleSeasonSupported;
	}
	
	private String name;
	private boolean singleSeasonSupported;
	private ImageIcon icon;
	
	
	public EpisodeListClient(String name, ImageIcon icon, boolean singleSeasonSupported) {
		this.name = name;
		this.icon = icon;
		this.singleSeasonSupported = singleSeasonSupported;
	}
	

	public String getName() {
		return name;
	}
	

	public ImageIcon getIcon() {
		return icon;
	}
	

	@Override
	public String toString() {
		return getName();
	}
	
}
