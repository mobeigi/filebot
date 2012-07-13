// filebot -script "http://filebot.sf.net/scripts/cleaner.groovy" [--action test] /path/to/media/

/*
 * Delete orphaned "clutter" files like nfo, jpg, etc and sample files
 */
def isClutter(f) {
	def blacklist = f.path =~ /\b(?i:sample|trailer|extras|deleted.scenes|music.video|scrapbook)\b/ && f.length() < 100 * 1024 * 1024  
	def extension = f.hasExtension("jpg", "jpeg", "png", "gif", "nfo", "xml", "htm", "html", "log", "srt", "sub", "idx", "md5", "sfv", "txt", "rtf", "url", "db", "dna")
	return blacklist || extension // path contains blacklisted terms or extension is blacklisted
}


def clean(f) {
	println "Delete $f"
	
	// do a dry run via --action test
	if (_args.action == 'test') { 
		return false
	}
	
	return f.isDirectory() ? f.deleteDir() : f.delete()
}


// delete clutter files in orphaned media folders
args.getFiles{ isClutter(it) && !it.dir.hasFile{ (it.isVideo() || it.isAudio()) && !isClutter(it) }}.each { clean(it) }

// delete empty folders but exclude given args
args.getFolders{ it.listFiles().length == 0 && !args.contains(it) }.each { clean(it) }
