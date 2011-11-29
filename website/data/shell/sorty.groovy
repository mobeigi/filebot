// Settings
def source = "X:/source"
def target = "Y:/target"

def episodeFormat = "{n}{'/Season '+s}/{episode}"
def movieFormat = "{movie}/{movie}"

def exclude(file) {
	file =~ /\p{Punct}chunk/
}

/*
 * Fetch subtitles and sort into folders
 */
"$source/TV".eachMediaFolder() { dir ->
	def files = dir.listFiles { !exclude(it) }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort episodes / subtitles
	rename(file:files, db:'TVRage', format:"$target/TV/$episodeFormat")
}

"$source/Movies".eachMediaFolder() { dir ->
	def files = dir.listFiles { !exclude(it) }
	
	// fetch subtitles
	files += getSubtitles(file:files)
	
	// sort movies / subtitles
	rename(file:files, db:'TheMovieDB', format:"$target/Movies/$movieFormat")
}
