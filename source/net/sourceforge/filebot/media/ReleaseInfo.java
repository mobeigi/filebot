
package net.sourceforge.filebot.media;


import static java.util.ResourceBundle.*;
import static java.util.regex.Pattern.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
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
	
	
	public List<String> cleanRelease(Iterable<String> items) throws IOException {
		return clean(items, getReleaseGroupPattern(), getLanguageSuffixPattern(), getVideoSourcePattern(), getVideoFormatPattern(), getResolutionPattern(), getBlacklistPattern());
	}
	
	
	public String cleanRelease(String item) throws IOException {
		return clean(item, getReleaseGroupPattern(), getLanguageSuffixPattern(), getVideoSourcePattern(), getVideoFormatPattern(), getResolutionPattern(), getBlacklistPattern());
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
	
	
	public Pattern getLanguageSuffixPattern() {
		Set<String> tokens = new TreeSet<String>();
		
		for (String code : Locale.getISOLanguages()) {
			Locale locale = new Locale(code);
			tokens.add(locale.getLanguage());
			tokens.add(locale.getISO3Language());
			tokens.add(locale.getDisplayLanguage(Locale.ENGLISH));
		}
		
		// remove illegal tokens
		tokens.remove("");
		
		// .{language}[.srt]
		return compile("(?<=[.])(" + join(tokens, "|") + ")(?=$)", CASE_INSENSITIVE);
	}
	
	
	public Pattern getResolutionPattern() {
		// match screen resolutions 640x480, 1280x720, etc
		return compile("(?<!\\p{Alnum})(\\d{4}|[6-9]\\d{2})x(\\d{4}|[4-9]\\d{2})(?!\\p{Alnum})");
	}
	
	
	public Pattern getVideoFormatPattern() {
		// pattern matching any video source name
		String pattern = getBundle(getClass().getName()).getString("pattern.video.format");
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
	
	
	public Pattern getBlacklistPattern() throws IOException {
		// pattern matching any release group name enclosed in separators
		return compile("(?<!\\p{Alnum})(" + join(blacklistResource.get(), "|") + ")(?!\\p{Alnum})", CASE_INSENSITIVE);
	}
	
	
	// fetch release group names online and try to update the data every other day
	protected final PatternResource releaseGroupResource = new PatternResource(getBundle(getClass().getName()).getString("url.release-groups"));
	protected final PatternResource blacklistResource = new PatternResource(getBundle(getClass().getName()).getString("url.query-blacklist"));
	
	
	protected static class PatternResource extends CachedResource<String[]> {
		
		public PatternResource(String resource) {
			super(resource, String[].class, 24 * 60 * 60 * 1000); // 24h update interval
		}
		
		
		@Override
		public String[] process(ByteBuffer data) {
			return compile("\\n").split(Charset.forName("UTF-8").decode(data));
		}
	}
	
}
