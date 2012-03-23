// filebot -script "http://filebot.sf.net/scripts/artwork.tmdb.groovy" -trust-script /path/to/media/

/*
 * Fetch movie artwork. The movie is determined using the parent folders name.
 */

def fetchArtwork(outputFile, movieInfo, artworkType, artworkSize) {
	// select and fetch artwork
	def artwork = movieInfo.images.find { it.type == artworkType && it.size == artworkSize }
	if (artwork == null) {
		println "Artwork not found: $outputFile"
		return null
	}
	
	println "Fetching $outputFile => $artwork"
	return artwork.url.saveAs(outputFile)
}


def fetchNfo(outputFile, movieInfo) {
	movieInfo.applyXmlTemplate('''<movie>
			<title>$name</title>
			<year>$released.year</year>
			<rating>$rating</rating>
			<votes>$votes</votes>
			<plot>$overview</plot>
			<runtime>$runtime</runtime>
			<mpaa>$certification</mpaa>
			<genre>${!genres.empty ? genres[0] : ''}</genre>
			<id>tt${imdbId.pad(7)}</id>
		</movie>
	''').saveAs(outputFile)
}


def fetchMovieArtworkAndNfo(movieDir, movie) {
	println "Fetch nfo and artwork for $movie"
	def movieInfo = TheMovieDB.getMovieInfo(movie, Locale.ENGLISH)
	
	println movieInfo
	movieInfo.images.each {
		println "Available artwork: $it.url => $it"
	}
	
	// fetch nfo
	fetchNfo(movieDir['movie.nfo'], movieInfo)
	
	// fetch series banner, fanart, posters, etc
	fetchArtwork(movieDir['folder.jpg'], movieInfo, 'poster', 'original')
	fetchArtwork(movieDir['backdrop.jpg'], movieInfo, 'backdrop', 'original')
}


args.eachMediaFolder { dir ->
	def videos = dir.listFiles{ it.isVideo() }
	
	def query = _args.query ?: dir.name
	def options = TheMovieDB.searchMovie(query, _args.locale)
	if (options.isEmpty()) {
		println "Movie not found: $query"
		return null
	}
	
	// sort by relevance
	options = options.sortBySimilarity(query, { it.name })
	
	// auto-select series
	def movie = options[0]
	
	// maybe require user input
	if (options.size() != 1 && !_args.nonStrict && !java.awt.GraphicsEnvironment.headless) {
		movie = javax.swing.JOptionPane.showInputDialog(null, "Please select Movie:", dir.path, 3, null, options.toArray(), movie);
		if (movie == null) return null
	}
	
	println "$dir => $movie"
	try {
		fetchMovieArtworkAndNfo(dir, movie)
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
	}
}
