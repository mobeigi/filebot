// EXPERIMENTAL // HERE THERE BE DRAGONS

// PERSONALIZED SETTINGS
def episodeDir    = "V:/in/TV"
def episodeFormat = "V:/out/TV/{n}{'/Season '+s}/{episode}"
def movieDir      = "V:/in/Movies"
def movieFormat   = "V:/out/Movies/{movie}/{movie}"

// XBMC ON LOCAL MACHINE 
def xbmc = ['localhost'] // (use [] to not notify any XBMC instances about updates)



// ignore chunk, part, par and hidden files
def incomplete(f) { f.name =~ /[.]chunk|[.]part\d{0,3}$|[.]par$|[.]dat$/ || f.isHidden() }


// run cmdline unrar (require -trust-script) on multi-volume rar files
[episodeDir, movieDir].getFiles().findAll { it =~ /[.]part01[.]rar$/ || (it =~ /[.]rar$/ && !(it =~ /[.]part\d{2}[.]rar$/)) }.each { rar ->
	// new layout: foo.part1.rar, foo.part2.rar
	// old layout: foo.rar, foo.r00, foo.r01
	boolean partLayout = (rar =~ /[.]part01[.]rar/)
	
	// extract name from name.part01.rar or name.rar
	def name = rar.getName()[0 .. (partLayout ? -12 : -5)]
	
	// find all volumes of the same name
	def volumes = rar.getParentFile().listFiles{
		it =~ (partLayout ? /$name[.]part\d{2}[.]/ : /$name[.](r\d{2}|rar)/)
	}
	
	// find all incomplete volumes
	def incomplete = volumes.findAll{ incomplete(it) }
	
	// all volumes complete, call unrar on first volume
	if (incomplete.isEmpty()) {
		def exitCode = execute("unrar", "x", "-y", "-p-", rar.getAbsolutePath(), rar.getPathWithoutExtension() + "/")
		
		// delete all volumes after successful extraction
		if (exitCode == 0) {
			volumes*.delete()
		}
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
