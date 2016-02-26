package net.filebot.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EpisodeUtilities {

	public static List<Episode> filterBySeason(Iterable<Episode> episodes, int season) {
		List<Episode> results = new ArrayList<Episode>(25);

		// filter given season from all seasons
		for (Episode episode : episodes) {
			if (episode.getSeason() != null && season == episode.getSeason()) {
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

	public static Comparator<Episode> episodeComparator() {
		return NUMBERS_COMPARATOR;
	}

	public static final Comparator<Episode> NUMBERS_COMPARATOR = new Comparator<Episode>() {

		@Override
		public int compare(Episode a, Episode b) {
			int diff = compareValue(a.getSeason(), b.getSeason());
			if (diff != 0)
				return diff;

			diff = compareValue(a.getEpisode(), b.getEpisode());
			if (diff != 0)
				return diff;

			diff = compareValue(a.getSpecial(), b.getSpecial());
			if (diff != 0)
				return diff;

			return compareValue(a.getAbsolute(), b.getAbsolute());
		}

		private <T> int compareValue(Comparable<T> o1, T o2) {
			if (o1 == null && o2 == null)
				return 0;
			if (o1 == null && o2 != null)
				return Integer.MAX_VALUE;
			if (o1 != null && o2 == null)
				return Integer.MIN_VALUE;

			return o1.compareTo(o2);
		}
	};

	private EpisodeUtilities() {
		throw new UnsupportedOperationException();
	}
}
