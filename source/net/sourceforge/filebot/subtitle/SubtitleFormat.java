
package net.sourceforge.filebot.subtitle;


import java.util.Scanner;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;


public enum SubtitleFormat {
	
	SubRip {
		
		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubRipReader(new Scanner(readable));
		}
	},
	
	MicroDVD {
		
		@Override
		public SubtitleReader newReader(Readable readable) {
			return new MicroDVDReader(new Scanner(readable));
		}
	},
	
	SubViewer {
		
		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubViewerReader(new Scanner(readable));
		}
	},
	
	SubStationAlpha {
		
		@Override
		public SubtitleReader newReader(Readable readable) {
			return new SubStationAlphaReader(new Scanner(readable));
		}
	};
	
	public abstract SubtitleReader newReader(Readable readable);
	

	public ExtensionFileFilter filter() {
		return MediaTypes.getDefault().filter("subtitle/" + this);
	}
	
}
