// filebot -script "http://filebot.sf.net/scripts/renall.groovy" <options> <folder>

/*
 * Rename all tv shows, anime or movies using given or default options  
 */
args.eachMediaFolder { 
	rename(folder:it)
}
