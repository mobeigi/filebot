#!/usr/bin/env filebot -script

def recentMoviesFile = new File('recent-movies.txt')
def recentMoviesIndex = (recentMoviesFile.exists() ? recentMoviesFile.readLines('UTF-8') : []) as TreeSet

def toDate = LocalDate.now()
def fromDate = LocalDate.now().minus(Period.ofDays(30))

TheMovieDB.discover(fromDate, toDate, Locale.ENGLISH).each{ m ->
	if (recentMoviesIndex.add([m.tmdbId.pad(6), m.year, m.name].join('\t'))) {
		println m
	}
}

recentMoviesIndex.join('\n').saveAs(recentMoviesFile)
