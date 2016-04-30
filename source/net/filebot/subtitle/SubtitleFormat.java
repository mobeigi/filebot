
package net.filebot.subtitle;

import net.filebot.MediaTypes;
import net.filebot.util.FileUtilities.ExtensionFileFilter;

public enum SubtitleFormat {

	SubRip {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubRipReader(readable);
		}
	},

	MicroDVD {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new MicroDVDReader(readable);
		}
	},

	SubViewer {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubViewerReader(readable);
		}
	},

	SubStationAlpha {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubStationAlphaReader(readable);
		}
	};

	public abstract SubtitleReader newReader(Readable readable);

	public ExtensionFileFilter getFilter() {
		return MediaTypes.getDefaultFilter("subtitle/" + name());
	}

}
