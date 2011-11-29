// Settings
def episodeDir = "X:/in/TV"
def movieDir = "X:/in/Movies"

def episodeFormat = "X:/out/TV/{n}{'/Season '+s}/{episode}"
def movieFormat = "X:/out/Movies/{movie}/{movie}"

def exclude(f) { f =~ /\p{Punct}(chunk|part)/ }

// run cmdline unrar / unzip (require -trust-script)
[episodeDir, movieDir].getFiles().findAll{ !exclude(it) && it.hasExtension('zip') }.each {
	execute("unzip", it.getAbsolutePath());
}
[episodeDir, movieDir].getFiles().findAll{ !exclude(it) && it.hasExtension('rar') }.each {
	execute("unrar", "-x", it.getAbsolutePath());
}

/*
 * Fetch subtitles and sort into folders
 */
episodeDir.eachMediaFolder() { dir ->
	def files = dir.listFiles { !exclude(it) }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort episodes / subtitles
	rename(file:files, db:'TVRage', format:episodeFormat)
}

movieDir.eachMediaFolder() { dir ->
	def files = dir.listFiles { !exclude(it) }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort movies / subtitles
	rename(file:files, db:'OpenSubtitles', format:movieFormat)
}
