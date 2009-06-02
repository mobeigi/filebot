
package net.sourceforge.filebot.ui.panel.subtitle;


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
				public Map<String, ByteBuffer> extract() throws IOException {
					return Collections.emptyMap();
				}
			};
		}
	};


	public static ArchiveType forName(String name) {
		if (name == null)
			return UNDEFINED;
		
		if (name.equalsIgnoreCase("zip"))
			return ZIP;
		
		if (name.equalsIgnoreCase("rar"))
			return RAR;
		
		return UNDEFINED;
	}
	

	public abstract Archive fromData(ByteBuffer data);
	

	public String getExtension() {
		return toString().toLowerCase();
	}
	
}
