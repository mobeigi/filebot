// filebot -script "http://filebot.sf.net/scripts/sortivo.groovy" -trust-script <folder> [-non-strict] [--output path/to/folder]

/*
 * Move/Rename a mix of episodes and movies that are all in the same folder.
 */
def groups = args.getFiles().groupBy{
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
		return rename(file:files, format:'{n} - {s00e00} - {t}', db:'TheTVDB')
	}
	
	// MOVIE MODE
	if (group.mov && !group.tvs) {
		return rename(file:files, format:'{n} ({y}){" CD$pi"}', db:'TheMovieDB')
	}
}
