
package net.sourceforge.filebot.web;


import java.util.ArrayList;
import java.util.List;


final class EpisodeListUtilities {
	
	public static List<Episode> filterBySeason(Iterable<Episode> episodes, int season) {
		
		List<Episode> results = new ArrayList<Episode>(25);
		
		// filter given season from all seasons
		for (Episode episode : episodes) {
			if (season == episode.getSeason()) {
				results.add(episode);
			}
		}
		
		return results;
	}
	

	public static int getLastSeason(Iterable<Episode> episodes) {
		int lastSeason = 0;
		
		// filter given season from all seasons
		for (Episode episode : episodes) {
			if (episode.getSeason() != null && episode.getSeason() > lastSeason) {
				lastSeason = episode.getSeason();
			}
		}
		
		return lastSeason;
	}
	

	private EpisodeListUtilities() {
		throw new UnsupportedOperationException();
	}
}
