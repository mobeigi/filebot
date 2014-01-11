package net.sourceforge.filebot.subtitle;

import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;

import net.sourceforge.filebot.web.SubtitleDescriptor;

public enum SubtitleNaming {

	ORIGINAL {

		@Override
		public String format(File video, SubtitleDescriptor subtitle, String ext) {
			return String.format("%s.%s", subtitle.getName(), ext);
		}

		@Override
		public String toString() {
			return "Keep Original";
		}
	},

	MATCH_VIDEO {

		@Override
		public String format(File video, SubtitleDescriptor subtitle, String ext) {
			return SubtitleUtilities.formatSubtitle(getName(video), null, ext);
		}

		@Override
		public String toString() {
			return "Match Video";
		}
	},

	MATCH_VIDEO_ADD_LANGUAGE_TAG {

		@Override
		public String format(File video, SubtitleDescriptor subtitle, String ext) {
			return SubtitleUtilities.formatSubtitle(getName(video), subtitle.getLanguageName(), ext);
		}

		@Override
		public String toString() {
			return "Match Video and Language";
		}
	};

	public abstract String format(File video, SubtitleDescriptor subtitle, String ext);

	public static SubtitleNaming forName(String s) {
		for (SubtitleNaming it : values()) {
			if (it.name().equalsIgnoreCase(s) || it.toString().equalsIgnoreCase(s)) {
				return it;
			}
		}
		return null;
	}

}
