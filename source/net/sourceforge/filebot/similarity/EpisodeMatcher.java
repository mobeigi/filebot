package net.sourceforge.filebot.similarity;

import static java.util.Collections.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.MultiEpisode;

public class EpisodeMatcher extends Matcher<File, Object> {

	public EpisodeMatcher(Collection<File> values, Collection<Episode> candidates, boolean strict) {
		// use strict matcher as to force a result from the final top similarity set
		super(values, candidates, strict, strict ? StrictEpisodeMetrics.defaultSequence(false) : EpisodeMetrics.defaultSequence(false));
	}

	@Override
	protected void deepMatch(Collection<Match<File, Object>> possibleMatches, int level) throws InterruptedException {
		Map<File, List<Episode>> episodeSets = new IdentityHashMap<File, List<Episode>>();
		for (Match<File, Object> it : possibleMatches) {
			List<Episode> episodes = episodeSets.get(it.getValue());
			if (episodes == null) {
				episodes = new ArrayList<Episode>();
				episodeSets.put(it.getValue(), episodes);
			}
			episodes.add((Episode) it.getCandidate());
		}

		Map<File, Set<SxE>> episodeIdentifierSets = new IdentityHashMap<File, Set<SxE>>();
		for (Entry<File, List<Episode>> it : episodeSets.entrySet()) {
			Set<SxE> sxe = new HashSet<SxE>(it.getValue().size());
			for (Episode ep : it.getValue()) {
				sxe.add(new SxE(ep.getSeason(), ep.getEpisode()));
			}
			episodeIdentifierSets.put(it.getKey(), sxe);
		}

		boolean modified = false;
		for (Match<File, Object> it : possibleMatches) {
			File file = it.getValue();
			Set<SxE> uniqueFiles = parseEpisodeIdentifer(file);
			Set<SxE> uniqueEpisodes = episodeIdentifierSets.get(file);

			if (uniqueFiles.equals(uniqueEpisodes)) {
				Episode[] episodes = episodeSets.get(file).toArray(new Episode[0]);

				if (isMultiEpisode(episodes)) {
					MultiEpisode episode = new MultiEpisode(episodes);
					disjointMatchCollection.add(new Match<File, Object>(file, episode));
					modified = true;
				}
			}
		}

		if (modified) {
			removeCollected(possibleMatches);
		}

		super.deepMatch(possibleMatches, level);

	}

	private final SeasonEpisodeMatcher seasonEpisodeMatcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, false);
	private final Map<File, Set<SxE>> transformCache = synchronizedMap(new HashMap<File, Set<SxE>>(64, 4));

	private Set<SxE> parseEpisodeIdentifer(File file) {
		Set<SxE> result = transformCache.get(file);
		if (result != null) {
			return result;
		}

		List<SxE> sxe = seasonEpisodeMatcher.match(file.getName());
		if (sxe != null) {
			result = new HashSet<SxE>(sxe);
		} else {
			result = emptySet();
		}

		transformCache.put(file, result);
		return result;
	}

	private boolean isMultiEpisode(Episode[] episodes) {
		// check episode sequence integrity
		Integer seqIndex = null;
		for (Episode ep : episodes) {
			if (seqIndex != null && !ep.getEpisode().equals(seqIndex + 1))
				return false;

			seqIndex = ep.getEpisode();
		}

		// check drill-down integrity
		String seriesName = null;
		for (Episode ep : episodes) {
			if (seriesName != null && !seriesName.equals(ep.getSeriesName()))
				return false;

			seriesName = ep.getSeriesName();
		}

		return true;
	}

}
