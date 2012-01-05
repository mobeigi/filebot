// filebot -script "http://filebot.sf.net/scripts/rsam.groovy" <options> <folder>


def isMatch(a, b) { similarity(a, b) > 0.9 }

/*
 * Rename anime, tv shows or movies (assuming each folder represents one item)
 */
args.eachMediaFolder { dir ->
	def n = dir.name
		
	[	[db:anidb,      query:{ anidb.search(n, _args.locale).find{ isMatch(it, n) } }],
		[db:thetvdb,    query:{ thetvdb.search(n, _args.locale).find{ isMatch(it, n) } }],
		[db:themoviedb, query:{ themoviedb.searchMovie(n, _args.locale).find{ isMatch(it, n) } }]
	].find {
		def match = it.query()
		if (match) { rename(folder:dir, db:it.db.name, query:match.name) }
		return match
	}
}
