// filebot -script "http://filebot.sourceforge.net/data/shell/mvrn.groovy" --format "{n}/{n} - {'S'+s.pad(2)}E{e.pad(2)} - {t}" --db thetvdb <source folder> <destination folder>

// sanity check
require { args.size == 2 && _args.format && _args.db }

// handle arguments
def source = args[0]
def destination = args[1] + _args.format

println 'Source Folder: ' + source
println 'Target Format: ' + destination.path

/*
 * Move/Rename videos from source folder into destination folder
 */
source.eachMediaFolder {
	println 'Processing ' + it
	rename(folder:it, format:destination.path, db:_args.db)
}
