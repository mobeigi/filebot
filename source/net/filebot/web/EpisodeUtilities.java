package net.filebot.web;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import net.filebot.WebServices;

public final class EpisodeUtilities {

	public static List<Episode> getMultiEpisodeList(Episode e) {
		return e instanceof MultiEpisode ? ((MultiEpisode) e).getEpisodes() : singletonList(e);
	}

	public static boolean isAnime(Episode e) {
		return WebServices.AniDB.getIdentifier().equals(e.getSeriesInfo().getDatabase());
	}

	public static boolean isRegular(Episode e) {
		return e.getEpisode() != null && e.getSpecial() == null;
	}

	public static boolean isAbsolute(Episode e) {
		return e.getAbsolute() != null && e.getSeriesInfo().getOrder() != null && SortOrder.Absolute == SortOrder.valueOf(e.getSeriesInfo().getOrder());
	}

	public static Episode getEpisodeByAbsoluteNumber(Episode e, EpisodeListProvider service, SortOrder order) throws Exception {
		// e.g. match AniDB episode to TheTVDB episode
		Set<String> seriesNames = getLenientSeriesNameSet(e);
		Locale locale = new Locale(e.getSeriesInfo().getLanguage());

		// episode may be a multi-episode
		List<Episode> multiEpisode = getMultiEpisodeList(e);

		for (SearchResult series : service.search(e.getSeriesName(), locale)) {
			// sanity check
			if (!series.getEffectiveNames().stream().anyMatch(seriesNames::contains)) {
				continue;
			}

			// match by absolute number or airdate if possible, default to absolute number otherwise
			List<Episode> airdateEpisodeList = service.getEpisodeList(series, order, locale);
			List<Episode> airdateEpisode = multiEpisode.stream().flatMap(abs -> {
				return airdateEpisodeList.stream().filter(sxe -> abs.getSpecial() == null && sxe.getSpecial() == null).filter(sxe -> {
					return abs.getAbsolute() != null && abs.getAbsolute().equals(sxe.getAbsolute());
				});
			}).collect(toList());

			// sanity check
			if (airdateEpisode.size() != multiEpisode.size()) {
				break;
			}

			return airdateEpisode.size() == 1 ? airdateEpisode.get(0) : new MultiEpisode(airdateEpisode);
		}

		// return episode object as is by default
		return e;
	}

	private static Set<String> getLenientSeriesNameSet(Episode e) {
		// use maximum strength collator by default
		Collator collator = Collator.getInstance(new Locale(e.getSeriesInfo().getLanguage()));
		collator.setDecomposition(Collator.FULL_DECOMPOSITION);
		collator.setStrength(Collator.PRIMARY);

		Set<String> seriesNames = new TreeSet<String>(collator);
		seriesNames.addAll(e.getSeriesNames());
		return seriesNames;
	}

	public static List<Episode> filterBySeason(Collection<Episode> episodes, int season) {
		return episodes.stream().filter(it -> {
			return it.getSeason() != null && season == it.getSeason();
		}).collect(toList());
	}

	public static int getLastSeason(Collection<Episode> episodes) {
		return episodes.stream().mapToInt(it -> {
			return it.getSeason() == null ? 0 : it.getSeason();
		}).max().orElse(0);
	}

	public static Comparator<Episode> episodeComparator() {
		return EPISODE_NUMBERS_COMPARATOR;
	}

	public static final Comparator<Episode> EPISODE_NUMBERS_COMPARATOR = new Comparator<Episode>() {

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
