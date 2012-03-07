
package net.sourceforge.filebot.cli;


import java.io.File;
import java.io.IOException;

import net.sourceforge.tuned.FileUtilities;


public enum StandardRenameAction implements RenameAction {
	
	MOVE {
		
		@Override
		public File rename(File from, File to) throws Exception {
			return FileUtilities.moveRename(from, to);
		}
	},
	
	COPY {
		
		@Override
		public File rename(File from, File to) throws Exception {
			return FileUtilities.copyAs(from, to);
		}
	},
	
	SYMLINK {
		
		@Override
		public File rename(File from, File to) throws Exception {
			File destionation = FileUtilities.resolveDestination(from, to);
			
			// create symlink via NIO.2
			try {
				java.nio.file.Files.createSymbolicLink(destionation.toPath(), from.toPath());
			} catch (LinkageError e) {
				throw new Exception("Unsupported Operation: createSymbolicLink");
			}
			
			return destionation;
		}
	},
	
	HARDLINK {
		
		@Override
		public File rename(File from, File to) throws Exception {
			File destionation = FileUtilities.resolveDestination(from, to);
			
			// create hardlink via NIO.2
			try {
				java.nio.file.Files.createLink(destionation.toPath(), from.toPath());
			} catch (LinkageError e) {
				throw new Exception("Unsupported Operation: createLink");
			}
			
			return destionation;
		}
	},
	
	TEST {
		
		@Override
		public File rename(File from, File to) throws IOException {
			return FileUtilities.resolveDestination(from, to);
		}
	};
	
	public static StandardRenameAction forName(String action) {
		for (StandardRenameAction it : values()) {
			if (it.name().equalsIgnoreCase(action))
				return it;
		}
		
		throw new IllegalArgumentException("Illegal rename action: " + action);
	}
}
