// filebot -script "http://filebot.sourceforge.net/data/shell/src.groovy" <folder>

/*
 * Fetch subtitles, rename and calculate checksums for all video files
 */
args.eachMediaFolder {
	getMissingSubtitles(folder:it)
	def renamedFiles = rename(folder:it)
	compute(file:renamedFiles.findAll{ it.isVideo() })
}
