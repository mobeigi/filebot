// filebot -script "http://filebot.sourceforge.net/data/shell/cleaner.groovy" -trust-script /path/to/media/

/*
 * Delete orphaned "clutter" files like nfo, jpg, etc
 */
def isClutter(file) {
	return file.hasExtension("nfo", "txt", "jpg", "jpeg")
}

// delete clutter files in orphaned media folders
args.getFiles{ isClutter(it) && !it.dir.hasFile{ it.isVideo() }}.each {
	println "Delete file $it: " + it.delete()
}

// delete empty folders but exclude roots
args.getFolders{ it.getFiles().isEmpty() && !args.contains(it) }.each {
	println "Delete dir $it: " + it.deleteDir()
}
