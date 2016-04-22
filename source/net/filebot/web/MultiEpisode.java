package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.util.Arrays;
import java.util.List;

public class MultiEpisode extends Episode {

	protected Episode[] episodes;

	protected MultiEpisode() {
		// used by serializer
	}

	public MultiEpisode(Episode... episodes) {
		super(episodes[0]);
		this.episodes = episodes.clone();
	}

	public MultiEpisode(List<Episode> episodes) {
		super(episodes.get(0));
		this.episodes = episodes.toArray(new Episode[0]);
	}

	public List<Episode> getEpisodes() {
		return unmodifiableList(asList(episodes));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MultiEpisode) {
			MultiEpisode other = (MultiEpisode) obj;
			return Arrays.equals(episodes, other.episodes);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(episodes);
	}

	@Override
	public MultiEpisode clone() {
		return new MultiEpisode(episodes);
	}

	@Override
	public String toString() {
		return EpisodeFormat.SeasonEpisode.formatMultiEpisode(getEpisodes());
	}

}
