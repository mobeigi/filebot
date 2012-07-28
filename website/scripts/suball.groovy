// filebot -script fn:suball <options> <folder>

/*
 * Get subtitles for all your media files  
 */
args.eachMediaFolder { 
	getMissingSubtitles(folder:it)
}
