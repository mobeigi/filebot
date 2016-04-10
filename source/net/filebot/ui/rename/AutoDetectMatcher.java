
package net.filebot.ui.rename;

import static java.util.Collections.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.WebServices.*;

import java.awt.Component;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Stream;

import net.filebot.media.AutoDetection;
import net.filebot.media.AutoDetection.Group;
import net.filebot.media.AutoDetection.Type;
import net.filebot.similarity.Match;
import net.filebot.web.SortOrder;

class AutoDetectMatcher implements AutoCompleteMatcher {

	private AutoCompleteMatcher movie = new MovieMatcher(TheMovieDB);
	private AutoCompleteMatcher episode = new EpisodeListMatcher(TheTVDB, false);
	private AutoCompleteMatcher anime = new EpisodeListMatcher(AniDB, false);
	private AutoCompleteMatcher music = new MusicMatcher(MediaInfoID3, AcoustID);

	@Override
	public List<Match<File, ?>> match(Collection<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		Map<Group, Set<File>> groups = new AutoDetection(files, false, locale).group();

		// can't use parallel stream because default fork/join pool doesn't play well with the security manager
		ExecutorService workerThreadPool = Executors.newFixedThreadPool(getPreferredThreadPoolSize());
		try {
			Map<Group, Future<List<Match<File, ?>>>> matches = groups.entrySet().stream().collect(toMap(Entry::getKey, it -> {
				return workerThreadPool.submit(() -> match(it.getKey(), it.getValue(), strict, order, locale, autodetection, parent));
			}));

			// collect results
			return matches.entrySet().stream().flatMap(it -> {
				try {
					return it.getValue().get().stream();
				} catch (Exception e) {
					log.log(Level.WARNING, "Failed group: " + it.getKey(), e);
					return Stream.empty();
				}
			}).sorted(comparing(Match::getValue, new OriginalOrder<File>(files))).collect(toList());
		} finally {
			workerThreadPool.shutdownNow();
		}
	}

	private List<Match<File, ?>> match(Group group, Collection<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		if (group.types().length == 1) {
			for (Type key : group.types()) {
				switch (key) {
				case Movie:
					return movie.match(files, strict, order, locale, autodetection, parent);
				case Series:
					return episode.match(files, strict, order, locale, autodetection, parent);
				case Anime:
					return anime.match(files, strict, order, locale, autodetection, parent);
				case Music:
					return music.match(files, strict, order, locale, autodetection, parent);
				}
			}
		}

		debug.info(format("Ignore group: %s", group));
		return emptyList();
	}

}
