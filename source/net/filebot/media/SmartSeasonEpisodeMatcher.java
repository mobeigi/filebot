package net.filebot.media;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import net.filebot.similarity.SeasonEpisodeMatcher;

public class SmartSeasonEpisodeMatcher extends SeasonEpisodeMatcher {

	// make sure certain patterns like x264 or 720p will never be interpreted as SxE numbers
	private Pattern ignorePattern = new ReleaseInfo().getVideoFormatPattern(false);

	public SmartSeasonEpisodeMatcher(SeasonEpisodeFilter sanity, boolean strict) {
		super(sanity, strict);
	}

	public SmartSeasonEpisodeMatcher(boolean strict) {
		super(DEFAULT_SANITY, strict);
	}

	protected String clean(CharSequence name) {
		return ignorePattern.matcher(name).replaceAll("");
	}

	@Override
	public List<SxE> match(CharSequence name) {
		return super.match(clean(name));
	}

	@Override
	protected List<String> tokenizeTail(File file) {
		List<String> tail = super.tokenizeTail(file);
		for (int i = 0; i < tail.size(); i++) {
			tail.set(i, clean(tail.get(i)));
		}
		return tail;
	}

}
