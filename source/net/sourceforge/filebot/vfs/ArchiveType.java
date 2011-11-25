
package net.sourceforge.filebot.vfs;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;


public enum ArchiveType {
	
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
			for (ArchiveType type : EnumSet.of(ZIP, RAR)) {
				try {
					Iterable<MemoryFile> files = type.fromData(data);
					if (files.iterator().hasNext()) {
						return files;
					}
				} catch (Exception e) {
					// ignore
				}
			}
			
			// cannot extract data, return empty archive
			return Collections.emptySet();
		}
	},
	
	UNKOWN {
		
		@Override
		public Iterable<MemoryFile> fromData(ByteBuffer data) {
			// cannot extract data, return empty archive
			return Collections.emptySet();
		}
	};
	
	public static ArchiveType forName(String name) {
		if (name == null)
			return UNDEFINED;
		
		if ("zip".equalsIgnoreCase(name))
			return ZIP;
		
		if ("rar".equalsIgnoreCase(name))
			return RAR;
		
		return UNKOWN;
	}
	

	public abstract Iterable<MemoryFile> fromData(ByteBuffer data);
	
}
