
package net.sourceforge.filebot.similarity;


import static java.util.Collections.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.MultiEpisode;


public class EpisodeMatcher extends Matcher<File, Object> {
	
	public EpisodeMatcher(Collection<File> values, Collection<Episode> candidates, boolean strictMetrics) {
		super(values, candidates, true, strictMetrics ? StrictEpisodeMetrics.defaultSequence(false) : EpisodeMetrics.defaultSequence(false));
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
		
		for (Iterator<Match<File, Object>> itr = possibleMatches.iterator(); itr.hasNext();) {
			File file = itr.next().getValue();
			Set<SxE> uniqueFiles = parseEpisodeIdentifer(file);
			Set<SxE> uniqueEpisodes = episodeIdentifierSets.get(file);
			
			if (uniqueFiles.equals(uniqueEpisodes)) {
				MultiEpisode episode = new MultiEpisode(episodeSets.get(file).toArray(new Episode[0]));
				disjointMatchCollection.add(new Match<File, Object>(file, episode));
				itr.remove();
			}
		}
		
		super.deepMatch(possibleMatches, level);
	}
	
	
	private final SeasonEpisodeMatcher seasonEpisodeMatcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, true);
	private final Map<File, Set<SxE>> transformCache = synchronizedMap(new WeakHashMap<File, Set<SxE>>(64, 4));
	
	
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
	
}
