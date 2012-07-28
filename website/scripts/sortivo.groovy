// filebot -script fn:sortivo <folder> --output path/to/folder [-non-strict]

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
		def norm = { s -> s.lower().space(' ') }
		def fn = norm(f.nameWithoutExtension)
		def sn = norm(tvs)
		def mn = norm(mov.name)
		
		// S00E00 | 2012.07.21 | One Piece 217 | Firefly - Serenity | [Taken 1, Taken 2, Taken 3, Taken 4, ..., Taken 10]
		if (parseEpisodeNumber(fn, true) || parseDate(fn) || (fn =~ sn && parseEpisodeNumber(fn.after(sn), false)) || fn.after(sn) =~ / - .+/ || f.dir.listFiles{ it.isVideo() && norm(it.name) =~ sn && it.name =~ /\b\d{1,3}\b/}.size() >= 10) {
			println "Exclude Movie: $mov"
			mov = null
		} else if ((detectMovie(f, true) && fn =~ /(19|20)\d{2}/) || (fn =~ mn && !(fn.after(mn) =~ /\b\d{1,3}\b/))) {
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
