
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;


enum ArchiveType {
	ZIP {
		
		@Override
		public Archive fromData(ByteBuffer data) {
			return new ZipArchive(data);
		}
	},
	
	RAR {
		
		@Override
		public Archive fromData(ByteBuffer data) {
			return new RarArchive(data);
		}
	},
	
	UNDEFINED {
		
		@Override
		public Archive fromData(ByteBuffer data) {
			// cannot extract data, return empty archive
			return new Archive() {
				
				@Override
				public Map<File, ByteBuffer> extract() throws IOException {
					return Collections.emptyMap();
				}
			};
		}
	};


	public static ArchiveType forName(String name) {
		if ("zip".equalsIgnoreCase(name))
			return ZIP;
		
		if ("rar".equalsIgnoreCase(name))
			return RAR;
		
		return UNDEFINED;
	}
	

	public abstract Archive fromData(ByteBuffer data);
	
}
