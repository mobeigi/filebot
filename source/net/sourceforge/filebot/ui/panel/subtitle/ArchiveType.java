
package net.sourceforge.filebot.ui.panel.subtitle;


import java.nio.ByteBuffer;
import java.util.Collections;


enum ArchiveType {
	ZIP {
		
		@Override
		public Iterable<MemoryFile> fromData(ByteBuffer data) {
			return new ZipArchive(data);
		}
	},
	
	RAR {
		
		@Override
		public Iterable<MemoryFile> fromData(ByteBuffer data) {
			return new RarArchive(data);
		}
	},
	
	UNDEFINED {
		
		@Override
		public Iterable<MemoryFile> fromData(ByteBuffer data) {
			// cannot extract data, return empty archive
			return Collections.emptySet();
		}
	};


	public static ArchiveType forName(String name) {
		if ("zip".equalsIgnoreCase(name))
			return ZIP;
		
		if ("rar".equalsIgnoreCase(name))
			return RAR;
		
		return UNDEFINED;
	}
	

	public abstract Iterable<MemoryFile> fromData(ByteBuffer data);
	
}
