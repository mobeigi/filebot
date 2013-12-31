
package net.sourceforge.filebot.subtitle;


import static java.lang.Math.*;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.MetricAvg;
import net.sourceforge.filebot.similarity.MetricCascade;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SequenceMatchSimilarity;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.Language;
import net.sourceforge.filebot.vfs.ArchiveType;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;


public final class SubtitleUtilities {
	
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
	
	
	public static List<SubtitleDescriptor> findSubtitles(SubtitleProvider service, Collection<String> querySet, String languageName) throws Exception {
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
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
