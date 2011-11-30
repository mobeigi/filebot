// Settings
def episodeDir = "X:/in/TV"
def movieDir = "X:/in/Movies"

def episodeFormat = "X:/out/TV/{n}{'/Season '+s}/{episode}"
def movieFormat = "X:/out/Movies/{movie}/{movie}"

def incomplete(f) { f =~ /[.]chunk|[.]part$/ }

// run cmdline unrar (require -trust-script) on multi-volume rar files (foo.part1.rar, foo.part2.rar, ...)
[episodeDir, movieDir].getFiles().findAll{ it =~ /[.]part01[.]rar$/ }.each { rarP1 ->
	// extract name from name.part01.rar
	def name = rarP1.getName()[0 .. -12];
	
	// find all volumes of the same name
	def volumes = rarP1.getParentFile().listFiles{ it.getName().startsWith(name) && it =~ /[.]part\d{2}[.]rar/ }
	
	// find all incomplete volumes
	def incomplete = volumes.findAll{ incomplete(it) }
	
	// all volumes complete, call unrar on first volume
	if (incomplete.isEmpty()) {
		execute("unrar", "x", "-y", "-p-", rarP1.getAbsolutePath(), rarP1.getPathWithoutExtension() + "/")
	}
}

/*
 * Fetch subtitles and sort into folders
 */
episodeDir.eachMediaFolder() { dir ->
	def files = dir.listFiles { !incomplete(it) }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort episodes / subtitles
	rename(file:files, db:'TVRage', format:episodeFormat)
}

movieDir.eachMediaFolder() { dir ->
	def files = dir.listFiles { !incomplete(it) }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort movies / subtitles
	rename(file:files, db:'OpenSubtitles', format:movieFormat)
}
