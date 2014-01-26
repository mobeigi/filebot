package net.sourceforge.filebot.subtitle;

import static java.lang.Math.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.filebot.similarity.EpisodeMetrics.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.filebot.Language;
import net.sourceforge.filebot.similarity.EpisodeMetrics;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.MetricAvg;
import net.sourceforge.filebot.similarity.MetricCascade;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SequenceMatchSimilarity;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.vfs.ArchiveType;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;

public final class SubtitleUtilities {

	public static Map<File, List<SubtitleDescriptor>> findSubtitleMatches(SubtitleProvider service, Collection<File> fileSet, String languageName, String forceQuery, boolean addOptions, boolean strict) throws Exception {
		// ignore anything that is not a video
		fileSet = filter(fileSet, VIDEO_FILES);

		// ignore clutter files from processing
		fileSet = filter(fileSet, not(getClutterFileFilter()));

		// collect results
		Map<File, List<SubtitleDescriptor>> subtitlesByFile = new HashMap<File, List<SubtitleDescriptor>>();

		for (List<File> byMediaFolder : mapByMediaFolder(fileSet).values()) {
			for (Entry<String, List<File>> bySeries : mapBySeriesName(byMediaFolder, true, false, Locale.ENGLISH).entrySet()) {
				// allow early abort
				if (Thread.interrupted())
					throw new InterruptedException();

				// auto-detect query and search for subtitles
				Collection<String> querySet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				List<File> files = bySeries.getValue();

				if (forceQuery != null && forceQuery.length() > 0) {
					querySet.add(forceQuery);
				} else if (bySeries.getKey().length() > 0) {
					// use auto-detected series name as query
					querySet.add(bySeries.getKey());
				} else {
					for (File f : files) {
						List<String> queries = new ArrayList<String>();

						// might be a movie, auto-detect movie names
						if (!isEpisode(f.getPath(), true)) {
							for (Movie it : detectMovie(f, null, null, Locale.ENGLISH, strict)) {
								queries.add(it.getName());
							}
						}

						if (queries.isEmpty()) {
							queries.add(stripReleaseInfo(getName(f)));
						}

						querySet.addAll(queries);
					}
				}

				Set<SubtitleDescriptor> subtitles = findSubtitles(service, querySet, languageName);

				// allow early abort
				if (Thread.interrupted())
					throw new InterruptedException();

				// files by possible subtitles matches
				for (File file : files) {
					subtitlesByFile.put(file, new ArrayList<SubtitleDescriptor>());
				}

				// add other possible matches to the options
				SimilarityMetric sanity = EpisodeMetrics.verificationMetric();
				float minMatchSimilarity = strict ? 0.9f : 0.6f;

				// first match everything as best as possible, then filter possibly bad matches
				for (Entry<File, SubtitleDescriptor> it : matchSubtitles(files, subtitles, false).entrySet()) {
					if (sanity.getSimilarity(it.getKey(), it.getValue()) >= minMatchSimilarity) {
						subtitlesByFile.get(it.getKey()).add(it.getValue());
					}
				}

				// this could be very slow, lets hope at this point there is not much left due to positive hash matches
				for (File file : files) {
					// add matching subtitles
					for (SubtitleDescriptor it : subtitles) {
						// grab only the first best option unless we really want all options
						if (!addOptions && subtitlesByFile.get(file).size() >= 1)
							continue;

						// ignore if it's already been added
						if (subtitlesByFile.get(file).contains(it))
							continue;

						// ignore if we're sure that SxE is a negative match
						if (isEpisode(it.getName(), true) && isEpisode(file.getPath(), true) && EpisodeMetrics.EpisodeFunnel.getSimilarity(file, it) < 1)
							continue;

						// ignore if it's not similar enough
						if (sanity.getSimilarity(file, it) < minMatchSimilarity)
							continue;

						subtitlesByFile.get(file).add(it);
					}
				}
			}
		}

		return subtitlesByFile;
	}

	public static Map<File, SubtitleDescriptor> matchSubtitles(Collection<File> files, Collection<SubtitleDescriptor> subtitles, boolean strict) throws InterruptedException {
		Map<File, SubtitleDescriptor> subtitleByVideo = new LinkedHashMap<File, SubtitleDescriptor>();

		// optimize for generic media <-> subtitle matching
		SimilarityMetric[] metrics = new SimilarityMetric[] { EpisodeFunnel, EpisodeBalancer, NameSubstringSequence, new MetricCascade(NameSubstringSequence, Name), Numeric, new NameSimilarityMetric() };

		// subtitle verification metric specifically excluding SxE mismatches
		SimilarityMetric absoluteSeasonEpisode = new SimilarityMetric() {

			@Override
			public float getSimilarity(Object o1, Object o2) {
				float f = SeasonEpisode.getSimilarity(o1, o2);
				if (f == 0 && (getEpisodeIdentifier(o1.toString(), true) == null) == (getEpisodeIdentifier(o2.toString(), true) == null)) {
					return 0;
				}
				return f < 1 ? -1 : 1;
			}
		};
		SimilarityMetric sanity = new MetricCascade(absoluteSeasonEpisode, AirDate, new MetricAvg(NameSubstringSequence, Name), getMovieMatchMetric());

		// first match everything as best as possible, then filter possibly bad matches
		Matcher<File, SubtitleDescriptor> matcher = new Matcher<File, SubtitleDescriptor>(files, subtitles, false, metrics);

		for (Match<File, SubtitleDescriptor> it : matcher.match()) {
			if (sanity.getSimilarity(it.getValue(), it.getCandidate()) >= (strict ? 0.9f : 0.6f)) {
				subtitleByVideo.put(it.getValue(), it.getCandidate());
			}
		}

		return subtitleByVideo;
	}

	public static Set<SubtitleDescriptor> findSubtitles(SubtitleProvider service, Collection<String> querySet, String languageName) throws Exception {
		Set<SubtitleDescriptor> subtitles = new LinkedHashSet<SubtitleDescriptor>();

		// search for and automatically select movie / show entry
		Set<SearchResult> resultSet = new HashSet<SearchResult>();
		for (String query : querySet) {
			resultSet.addAll(findProbableSearchResults(query, service.search(query)));
		}

		// fetch subtitles for all search results
		for (SearchResult it : resultSet) {
			subtitles.addAll(service.getSubtitleList(it, languageName));
		}

		return subtitles;
	}

	protected static Collection<SearchResult> findProbableSearchResults(String query, Iterable<? extends SearchResult> searchResults) {
		// auto-select most probable search result
		Set<SearchResult> probableMatches = new LinkedHashSet<SearchResult>();

		// use name similarity metric
		SimilarityMetric metric = new MetricAvg(new SequenceMatchSimilarity(), new NameSimilarityMetric());

		// find probable matches using name similarity > threshold
		for (SearchResult result : searchResults) {
			if (metric.getSimilarity(query, removeTrailingBrackets(result.getName())) > 0.8f || result.getName().toLowerCase().startsWith(query.toLowerCase())) {
				probableMatches.add(result);
			}
		}

		return probableMatches;
	}

	public static SubtitleDescriptor getBestMatch(File file, Collection<SubtitleDescriptor> subtitles, boolean strict) {
		if (file == null || subtitles == null || subtitles.isEmpty()) {
			return null;
		}

		try {
			return matchSubtitles(singleton(file), subtitles, strict).entrySet().iterator().next().getValue();
		} catch (NoSuchElementException e) {
			return null;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Detect charset and parse subtitle file even if extension is invalid
	 */
	public static List<SubtitleElement> decodeSubtitles(MemoryFile file) throws IOException {
		// gather all formats, put likely formats first
		LinkedList<SubtitleFormat> likelyFormats = new LinkedList<SubtitleFormat>();

		for (SubtitleFormat format : SubtitleFormat.values()) {
			if (format.getFilter().accept(file.getName()))
				likelyFormats.addFirst(format);
			else
				likelyFormats.addLast(format);
		}

		// decode bytes
		String textfile = getText(file.getData());

		// decode subtitle file with the first reader that seems to work
		for (SubtitleFormat format : likelyFormats) {
			// reset reader to position 0
			SubtitleReader parser = format.newReader(new StringReader(textfile));

			if (parser.hasNext()) {
				// correct format found
				List<SubtitleElement> list = new ArrayList<SubtitleElement>(500);

				// read subtitle file
				while (parser.hasNext()) {
					list.add(parser.next());
				}

				return list;
			}
		}

		// unsupported subtitle format
		throw new IOException("Cannot read subtitle format");
	}

	public static ByteBuffer exportSubtitles(MemoryFile data, SubtitleFormat outputFormat, long outputTimingOffset, Charset outputEncoding) throws IOException {
		if (outputFormat != null && outputFormat != SubtitleFormat.SubRip) {
			throw new IllegalArgumentException("Format not supported");
		}

		// convert to target format and target encoding
		if (outputFormat == SubtitleFormat.SubRip) {
			// output buffer
			StringBuilder buffer = new StringBuilder(4 * 1024);
			SubRipWriter out = new SubRipWriter(buffer);

			for (SubtitleElement it : decodeSubtitles(data)) {
				if (outputTimingOffset != 0)
					it = new SubtitleElement(max(0, it.getStart() + outputTimingOffset), max(0, it.getEnd() + outputTimingOffset), it.getText());

				out.write(it);
			}

			return outputEncoding.encode(CharBuffer.wrap(buffer));
		}

		// only change encoding
		return outputEncoding.encode(getText(data.getData()));
	}

	public static SubtitleFormat getSubtitleFormat(File file) {
		for (SubtitleFormat it : SubtitleFormat.values()) {
			if (it.getFilter().accept(file))
				return it;
		}

		return null;
	}

	public static SubtitleFormat getSubtitleFormatByName(String name) {
		for (SubtitleFormat it : SubtitleFormat.values()) {
			// check by name
			if (it.name().equalsIgnoreCase(name))
				return it;

			// check by extension
			if (it.getFilter().acceptExtension(name))
				return it;
		}

		return null;
	}

	public static String formatSubtitle(String name, String languageName, String type) {
		StringBuilder sb = new StringBuilder(name);

		if (languageName != null) {
			String lang = Language.getISO3LanguageCodeByName(languageName);

			if (lang == null) {
				// we probably won't get here, but just in case
				lang = languageName.replaceAll("\\W", "");
			}

			sb.append('.').append(lang);
		}

		if (type != null) {
			sb.append('.').append(type);
		}

		return sb.toString();
	}

	public static MemoryFile fetchSubtitle(SubtitleDescriptor descriptor) throws Exception {
		ByteBuffer data = descriptor.fetch();

		// extract subtitles from archive
		ArchiveType type = ArchiveType.forName(descriptor.getType());

		if (type != ArchiveType.UNKOWN) {
			// extract subtitle from archive
			Iterator<MemoryFile> it = type.fromData(data).iterator();
			while (it.hasNext()) {
				MemoryFile entry = it.next();
				if (SUBTITLE_FILES.accept(entry.getName())) {
					return entry;
				}
			}
		}

		// assume that the fetched data is the subtitle
		return new MemoryFile(descriptor.getPath(), data);
	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private SubtitleUtilities() {
		throw new UnsupportedOperationException();
	}

}
