
package net.sourceforge.filebot.mediainfo;


import static java.util.ResourceBundle.*;
import static java.util.concurrent.TimeUnit.*;
import static java.util.regex.Pattern.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.web.CachedResource;


public class ReleaseInfo {
	
	public String getVideoSource(File file) {
		// check parent and itself for group names
		return matchLast(getVideoSourcePattern(), file.getParent(), file.getName());
	}
	

	public String getReleaseGroup(File file) throws IOException {
		// check parent and itself for group names
		return matchLast(getReleaseGroupPattern(), file.getParent(), file.getName());
	}
	

	protected String matchLast(Pattern pattern, CharSequence... sequence) {
		String lastMatch = null;
		
		for (CharSequence name : sequence) {
			if (name == null)
				continue;
			
			Matcher matcher = pattern.matcher(name);
			while (matcher.find()) {
				lastMatch = matcher.group();
			}
		}
		
		return lastMatch;
	}
	

	public List<String> clean(Iterable<String> items) throws IOException {
		return clean(items, getVideoSourcePattern(), getCodecPattern());
	}
	

	public String clean(String item) throws IOException {
		return clean(item, getVideoSourcePattern(), getCodecPattern());
	}
	

	public List<String> cleanRG(Iterable<String> items) throws IOException {
		return clean(items, getReleaseGroupPattern(), getVideoSourcePattern(), getCodecPattern());
	}
	

	public String cleanRG(String item) throws IOException {
		return clean(item, getReleaseGroupPattern(), getVideoSourcePattern(), getCodecPattern());
	}
	

	public List<String> clean(Iterable<String> items, Pattern... blacklisted) {
		List<String> cleanedItems = new ArrayList<String>();
		for (String it : items) {
			cleanedItems.add(clean(it, blacklisted));
		}
		
		return cleanedItems;
	}
	

	public String clean(String item, Pattern... blacklisted) {
		for (Pattern it : blacklisted) {
			item = it.matcher(item).replaceAll("");
		}
		
		return item.replaceAll("[\\p{Punct}\\p{Space}]+", " ").trim();
	}
	

	public Pattern getCodecPattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.codec");
		return compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	

	public Pattern getVideoSourcePattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.video.source");
		return compile("(?<!\\p{Alnum})(" + pattern + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	

	public Pattern getReleaseGroupPattern() throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(releaseGroupResource.get(), "|") + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	

	// fetch release group names online and try to update the data every other day
	protected final CachedResource<String[]> releaseGroupResource = new CachedResource<String[]>(getBundle(getClass().getName()).getString("url.release-groups"), DAYS.toMillis(2)) {
		
		@Override
		public String[] process(ByteBuffer data) {
			return compile("\\s+").split(Charset.forName("UTF-8").decode(data));
		}
	};
	
}
