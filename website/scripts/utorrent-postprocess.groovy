// filebot -script "http://filebot.sf.net/scripts/utorrent-postprocess.groovy" "%D\%N" --output "X:/media" --action copy --conflict override -non-strict -trust-script -Xxbmc=localhost
println "Input: $args"
println "Parameters: $_args.parameters"

def input = args.getFiles()

// extract archives if necessary
input += extract(file:input)

// process only media files
input = input.findAll{ it.isVideo() || it.isSubtitle() }


// group episodes/movies and rename according to XBMC standards
def groups = input.groupBy {
	def tvs = detectSeriesName(it)
	def mov = detectMovie(it, false)
	println "$it.name [series: $tvs, movie: $mov]"
	
	// DECIDE EPISODE VS MOVIE (IF NOT CLEAR)
	if (tvs && mov) {
		if (it.name =~ "(?i:$tvs - .+)" || parseEpisodeNumber(it.name) || parseDate(it.name)) {
			println "Exclude Movie: $mov"
			mov = null
		} else if (detectMovie(it, true)) {
			println "Exclude Series: $tvs"
			tvs = null
		}
	}
	return [tvs:tvs, mov:mov]
}

groups.each{ group, files ->
	// EPISODE MODE
	if (group.tvs && !group.mov) {
		return rename(file:files, format:'TV Shows/{n}/{episode.special ? "Special" : "Season "+s}/{n} - {episode.special ? "S00E"+special.pad(2) : s00e00} - {t}', db:'TheTVDB')
	}
	
	// MOVIE MODE
	if (group.mov && !group.tvs) {
		return rename(file:files, format:'Movies/{n} ({y}){" CD$pi"}{".$lang"}', db:'TheMovieDB')
	}
}



// make XBMC scan for new content
try {
	telnet(xbmc, 9090) { writer, reader ->
		def msg = '{"id":1,"method":"VideoLibrary.Scan","params":[],"jsonrpc":"2.0"}'
		writer.println(msg)
	}
} catch(e) {
	println "${e.class.simpleName}: ${e.message}"
}
