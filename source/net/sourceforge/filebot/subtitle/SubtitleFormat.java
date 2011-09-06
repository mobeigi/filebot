
package net.sourceforge.filebot.subtitle;


import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;


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
		return MediaTypes.getDefaultFilter("subtitle/" + this.name());
	}
	
}
