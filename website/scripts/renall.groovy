// filebot -script fn:renall <options> <folder> --def target=[file|folder]

/*
 * Rename all tv shows, anime or movies folder by folder using given or default options.
 */
def target = tryQuietly{ target } ?: 'file' // target files by default

args.eachMediaFolder {
	if (it.isDisk())
		return rename(file:it) // rename disk folders instead of files regardless of mode
	
	switch(target) {
		case 'file'   :   return rename(folder:it) // rename files within each folder
		case 'folder' :   return rename(file:it)   // rename folders as if they were files
	}
}
