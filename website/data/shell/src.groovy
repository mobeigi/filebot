// filebot -script "http://filebot.sourceforge.net/data/shell/src.groovy" <folder>

args.eachMediaFolder {
	getSubtitles(folder:it)
	rename(folder:it)
	compute(file:it.listFiles().findAll{ it.isVideo() })
}
