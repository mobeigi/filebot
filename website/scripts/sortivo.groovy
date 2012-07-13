// filebot -script "http://filebot.sf.net/scripts/sortivo.groovy" <folder> --output path/to/folder [-non-strict]

// process only media files
def input = args.getFiles{ it.isVideo() || it.isSubtitle() }

// ignore clutter files
input = input.findAll{ !(it.path =~ /\b(?i:sample|trailer|extras|deleted.scenes|music.video|scrapbook)\b/) }

// print input fileset
input.each{ println "Input: $it" }

/*
 * Move/Rename a mix of episodes and movies that are all in the same folder.
 */
def groups = input.groupBy{ f ->
	def tvs = detectSeriesName(f)
	def mov = (parseEpisodeNumber(f) || parseDate(f)) ? null : detectMovie(f, false) // skip movie detection if we can already tell it's an episode
	println "$f.name [series: $tvs, movie: $mov]"
	
	// DECIDE EPISODE VS MOVIE (IF NOT CLEAR)
	if (tvs && mov) {
		def fn = f.nameWithoutExtension.space(' ')
		if (fn =~ "(?i:$tvs - .+)" || parseEpisodeNumber(fn, true) || parseDate(fn)) {
			println "Exclude Movie: $mov"
			mov = null
		} else if (detectMovie(f, true) && (fn =~ /(19|20)\d{2}/ || !(tvs =~ "(?i:$mov.name)"))) {
			println "Exclude Series: $tvs"
			tvs = null
		} else if (fn =~ "(?i:$tvs)" && parseEpisodeNumber(fn.after(tvs), false)) {
			println "Exclude Movie: $mov"
			mov = null
		} else if (fn =~ "(?i:$mov.name)" && !parseEpisodeNumber(fn.after(mov.name), false)) {
			println "Exclude Series: $tvs"
			tvs = null
		}
	}
	
	return [tvs:tvs, mov:mov]
}

groups.each{ group, files ->
	// EPISODE MODE
	if (group.tvs && !group.mov) {
		rename(file:files, format:'TV Shows/{n}/{episode.special ? "Special" : "Season "+s}/{n} - {episode.special ? "S00E"+special.pad(2) : s00e00} - {t}', db:'TheTVDB')
	}
	
	// MOVIE MODE
	if (group.mov && !group.tvs) {
		rename(file:files, format:'Movies/{n} ({y})/{n} ({y}){" CD$pi"}', db:'TheMovieDB')
	}
}
