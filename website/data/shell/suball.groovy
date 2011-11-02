// filebot -script "http://filebot.sourceforge.net/data/shell/suball.groovy" <options> <folder>

/*
 * Get subtitles for all your media files  
 */
args.eachMediaFolder { 
	getSubtitles(folder:it)
}
