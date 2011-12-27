// filebot -script "http://filebot.sf.net/scripts/suball.groovy" <options> <folder>

/*
 * Get subtitles for all your media files  
 */
args.eachMediaFolder { 
	getMissingSubtitles(folder:it)
}
