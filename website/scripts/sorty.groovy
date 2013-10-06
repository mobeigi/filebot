// PERSONALIZED SETTINGS
def episodeDir    = '''/in/TV'''
def episodeFormat = '''/out/TV/{n}/{"Season ${s.pad(2)}"}/{n} - {s00e00} - {t}'''
def movieDir      = '''/in/Movies'''
def movieFormat   = '''/out/Movies/{n} ({y})/{n} ({y}){" CD$pi"}'''

// XBMC ON LOCAL MACHINE 
def xbmc = ['localhost'] // (use [] to not notify any XBMC instances about updates)



// ignore chunk, part, par and hidden files
def incomplete(f) { f.name =~ /[.]incomplete|[.]chunk|[.]par$|[.]dat$/ }


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
	rename(file:files, db:'TheTVDB', format:episodeFormat)
}

movieDir.getFolders{ !it.hasFile{ incomplete(it) } && it.hasFile{ it.isVideo() } }.each{ dir ->
	println "Processing $dir"
	def files = dir.listFiles{ it.isVideo() }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort movies / subtitles
	rename(file:files, db:'TheMovieDB', format:movieFormat)
}


// make XBMC scan for new content
xbmc.each { host ->
	telnet(host, 9090) { writer, reader ->
		// API call for latest XBMC release
		def msg = '{"id":1,"method":"VideoLibrary.Scan","params":[],"jsonrpc":"2.0"}'
		
		// API call for XBMC Dharma-Release or older
		// def msg = '{"id":1,"method":"VideoLibrary.ScanForContent","params":[],"jsonrpc":"2.0"}'
				
		writer.println(msg)
	}
}
