
package net.filebot.subtitle;

import net.filebot.MediaTypes;
import net.filebot.util.FileUtilities.ExtensionFileFilter;

public enum SubtitleFormat {

	SubRip {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubRipReader(readable);
		}

		@Override
		public ExtensionFileFilter getFilter() {
			return MediaTypes.getTypeFilter("subtitle/SubRip");
		}
	},

	MicroDVD {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new MicroDVDReader(readable);
		}

		@Override
		public ExtensionFileFilter getFilter() {
			return MediaTypes.getTypeFilter("subtitle/MicroDVD");
		}
	},

	SubViewer {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubViewerReader(readable);
		}

		@Override
		public ExtensionFileFilter getFilter() {
			return MediaTypes.getTypeFilter("subtitle/SubViewer");
		}
	},

	SubStationAlpha {

		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubStationAlphaReader(readable);
		}

		@Override
		public ExtensionFileFilter getFilter() {
			return MediaTypes.getTypeFilter("subtitle/SubStationAlpha");
		}
	};

	public abstract SubtitleReader newReader(Readable readable);

	public abstract ExtensionFileFilter getFilter();

}
