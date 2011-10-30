import net.sourceforge.filebot.similarity.*

def lang = Locale.ENGLISH
def isMatch(a, b) { new NameSimilarityMetric().getSimilarity(a, b) > 0.9 }

/*
 * Rename anime, tv shows or movies (assuming each folder represents one item)
 */
args.eachMediaFolder { dir ->
	def n = dir.getName()
	
	[	[db:anidb,      query:{ anidb.search(n, lang).find{ isMatch(it, n) } }],
		[db:thetvdb,    query:{ thetvdb.search(n, lang).find{ isMatch(it, n) } }],
		[db:themoviedb, query:{ themoviedb.searchMovie(n, lang).find{ isMatch(it, n) } }]
	].find {
		def match = it.query()
		if (match) { rename(folder:dir, db:it.db.getName(), query:match.getName()) }
		return match
	}
}
