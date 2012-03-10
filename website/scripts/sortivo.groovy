// filebot -script "http://filebot.sf.net/scripts/sortivo.groovy" <folder> [-non-strict] [--output path/to/folder]

/*
 * Move/Rename a mix of episodes and movies that are all in the same folder.
 */
args.getFiles{ it.isVideo() }.each{
	def tvs = detectSeriesName(it)
	def mov = detectMovie(it, false)
	
	println "$it.name [series: $tvs, movie: $mov]"
	
	// DECIDE EPISODE VS MOVIE (IF NOT CLEAR)
	if (tvs && mov) {
		if (it.name =~ "(?i:$tvs - .+)" || parseEpisodeNumber(it) || parseDate(it)) {
			println "Exclude Movie: $mov"
			mov = null
		} else if (detectMovie(it, true)) {
			println "Exclude Series: $tvs"
			tvs = null
		}
	}
	
	// EPISODE MODE
	if (tvs && !mov) {
		return rename(file:it, format:'{n} - {s00e00} - {t}', db:'TheTVDB')
	}
	
	// MOVIE MODE
	if (mov && !tvs) {
		return rename(file:it, format:'{n} ({y}){" CD$pi"}', db:'TheMovieDB')
	}
}
