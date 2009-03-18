
package net.sourceforge.filebot.web;


import java.util.ArrayList;
import java.util.List;


public final class EpisodeListUtilities {
	
	public static List<Episode> filterBySeason(Iterable<Episode> episodes, int season) {
		
		List<Episode> results = new ArrayList<Episode>(25);
		
		// filter given season from all seasons
		for (Episode episode : episodes) {
			try {
				if (season == Integer.parseInt(episode.getSeasonNumber())) {
					results.add(episode);
				}
			} catch (NumberFormatException e) {
				// ignore illegal episodes
			}
		}
		
		return results;
	}
	

	public static int getLastSeason(Iterable<Episode> episodes) {
		int lastSeason = 0;
		
		// filter given season from all seasons
		for (Episode episode : episodes) {
			try {
				lastSeason = Math.max(lastSeason, Integer.parseInt(episode.getSeasonNumber()));
			} catch (NumberFormatException e) {
				// ignore illegal episodes
			}
		}
		
		return lastSeason;
	}
	

	private EpisodeListUtilities() {
		throw new UnsupportedOperationException();
	}
}
