// filebot -script "http://filebot.sourceforge.net/data/shell/src.groovy" <folder>

/*
 * Fetch subtitles, rename and calculate checksums for all video files
 */
args.eachMediaFolder {
	getMissingSubtitles(folder:it)
	rename(folder:it)
	compute(file:it.listFiles().findAll{ it.isVideo() })
}
