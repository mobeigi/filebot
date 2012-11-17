
package net.sourceforge.filebot;


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
	
	KEEPLINK {
		
		@Override
		public File rename(File from, File to) throws Exception {
			File destionation = FileUtilities.resolveDestination(from, to, true);
			
			// move file and the create a symlink to the new location via NIO.2
			try {
				java.nio.file.Files.move(from.toPath(), destionation.toPath());
				java.nio.file.Files.createSymbolicLink(from.toPath(), destionation.toPath());
			} catch (LinkageError e) {
				throw new Exception("Unsupported Operation: move, createSymbolicLink");
			}
			
			return destionation;
		}
	},
	
	SYMLINK {
		
		@Override
		public File rename(File from, File to) throws Exception {
			File destionation = FileUtilities.resolveDestination(from, to, true);
			
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
			File destionation = FileUtilities.resolveDestination(from, to, true);
			
			// create hardlink via NIO.2
			try {
				java.nio.file.Files.createLink(destionation.toPath(), from.toPath());
			} catch (LinkageError e) {
				throw new Exception("Unsupported Operation: createLink");
			}
			
			return destionation;
		}
	},
	
	RENAME {
		
		@Override
		public File rename(File from, File to) throws IOException {
			// rename only the filename
			File dest = new File(from.getParentFile(), to.getName());
			
			if (!from.renameTo(dest))
				throw new IOException("Rename failed: " + dest);
			
			return dest;
		}
	},
	
	TEST {
		
		@Override
		public File rename(File from, File to) throws IOException {
			return FileUtilities.resolveDestination(from, to, false);
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
