// filebot -script fn:cleaner [--action test] /path/to/media/
def deleteRootFolder = tryQuietly{ root.toBoolean() }

/*
 * Delete orphaned "clutter" files like nfo, jpg, etc and sample files
 */
def isClutter(f) {
	// white list
	def ignore  = tryQuietly{ ignore }          ?: /extrathumbs/
	if (f.path =~ "(?i)\\b($ignore)\\b") return false
	
	// black list
	def exts    = tryQuietly{ exts }            ?: /jpg|jpeg|png|gif|nfo|xml|htm|html|log|srt|sub|idx|md5|sfv|txt|rtf|url|db|dna|log/
	def terms   = tryQuietly{ terms }           ?: /sample|trailer|extras|deleted.scenes|music.video|scrapbook/
	def maxsize = tryQuietly{ maxsize as Long } ?: 100 * 1024 * 1024
	
	// path contains blacklisted terms or extension is blacklisted
	return (f.extension ==~ "(?i)($exts)" || f.path =~ "(?i)\\b($terms)\\b") && f.length() < maxsize
}


def clean(f) {
	println "Delete $f"
	
	// do a dry run via --action test
	if (_args.action == 'test') {
		return false
	}
	
	return f.isDirectory() ? f.deleteDir() : f.delete()
}


// memoize media folder status for performance
def hasMediaFiles = { dir -> dir.getFiles().find{ (it.isVideo() || it.isAudio()) && !isClutter(it) } }.memoize()

// delete clutter files in orphaned media folders
args.getFiles{ isClutter(it) && !hasMediaFiles(it.dir) }.each { clean(it) }

// delete empty folders but exclude given args
args.getFolders().sort().reverse().each { if (it.listFiles().length == 0) { if (deleteRootFolder || !args.contains(it)) clean(it) } }
