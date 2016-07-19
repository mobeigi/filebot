#!/usr/bin/env filebot -script

def recentMoviesFile = new File('recent-movies.txt')
def recentMoviesIndex = new TreeMap()

if (recentMoviesFile.exists()) {
	recentMoviesFile.splitEachLine('\t', 'UTF-8') { line ->
		recentMoviesIndex.put(line[0] as int, line)
	}
}

def toDate = LocalDate.now()
def fromDate = LocalDate.now().minus(Period.ofDays(30))
def locale = Locale.ENGLISH

TheMovieDB.discover(fromDate, toDate, locale).each{ m ->
	if (!recentMoviesIndex.containsKey(m.tmdbId)) {
		def i = TheMovieDB.getMovieInfo(m, locale, false)

		if (i.imdbId == null)
			return

		def row = [i.id.pad(6), i.imdbId.pad(7), i.released.year as String, i.name]
		println row

		recentMoviesIndex.put(row[0] as int, row)
	}
}

recentMoviesIndex.values()*.join('\t').join('\n').saveAs(recentMoviesFile)
