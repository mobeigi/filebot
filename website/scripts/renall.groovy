// filebot -script fn:renall <options> <folder>

/*
 * Rename all tv shows, anime or movies using given or default options  
 */
args.eachMediaFolder { 
	rename(folder:it)
}
