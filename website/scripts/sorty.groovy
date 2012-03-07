// PERSONALIZED SETTINGS
def episodeDir    = "V:/in/TV"
def episodeFormat = "V:/out/TV/{n}{'/Season '+s}/{episode}"
def movieDir      = "V:/in/Movies"
def movieFormat   = "V:/out/Movies/{movie}/{movie}"

// XBMC ON LOCAL MACHINE 
def xbmc = ['localhost'] // (use [] to not notify any XBMC instances about updates)



// ignore chunk, part, par and hidden files
def incomplete(f) { f.name =~ /[.]incomplete|[.]chunk|[.]par$|[.]dat$/ || f.isHidden() }


// extract completed multi-volume rar files
[episodeDir, movieDir].getFolders{ !it.hasFile{ incomplete(it) } && it.hasFile{ it =~ /[.]rar$/ } }.each{ dir ->
	// extract all archives found in this folder
	def paths = extract(folder:dir)
	
	// delete original archive volumes after successful extraction
	if (paths != null && !paths.isEmpty()) {
		dir.listFiles{ it =~ /[.]rar$|[.]r[\d]+$/ }*.delete()
	}
}


/*
 * Fetch subtitles and sort into folders
 */
episodeDir.getFolders{ !it.hasFile{ incomplete(it) } && it.hasFile{ it.isVideo() } }.each{ dir ->
	println "Processing $dir"
	def files = dir.listFiles{ it.isVideo() }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort episodes / subtitles
	rename(file:files, db:'TVRage', format:episodeFormat)
}

movieDir.getFolders{ !it.hasFile{ incomplete(it) } && it.hasFile{ it.isVideo() } }.each{ dir ->
	println "Processing $dir"
	def files = dir.listFiles{ it.isVideo() }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort movies / subtitles
	rename(file:files, db:'OpenSubtitles', format:movieFormat)
}


// make XBMC scan for new content
xbmc.each { host ->
	telnet(host, 9090) { writer, reader ->
		writer.println('{"jsonrpc": "2.0", "method": "VideoLibrary.ScanForContent", "id": 1}')
	}
}
