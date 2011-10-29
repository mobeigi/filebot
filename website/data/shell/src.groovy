args.eachMediaFolder {
	getSubtitles(folder:it)
	rename(folder:it)
	compute(file:it.listFiles().findAll{ it.isVideo() })
}
