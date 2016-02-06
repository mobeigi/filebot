package net.filebot.web;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

public interface VideoHashSubtitleService {

	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] videoFiles, String languageName) throws Exception;

	public String getName();

	public URI getLink();

	public Icon getIcon();

	public CheckResult checkSubtitle(File videoFile, File subtitleFile) throws Exception;

	public void uploadSubtitle(Object identity, Locale locale, File[] videoFiles, File[] subtitleFiles) throws Exception;

	public static class CheckResult {
		public final boolean exists;
		public final Object identity;
		public final Locale language;

		public CheckResult(boolean exists, Object identity, Locale language) {
			this.exists = exists;
			this.identity = identity;
			this.language = language;
		}

		@Override
		public String toString() {
			return String.format("%s [%s] => %s", identity, language, exists);
		}
	}

}
