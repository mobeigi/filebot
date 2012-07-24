// filebot -script fn:artwork.tmdb /path/to/movies/

/*
 * Fetch movie artwork. The movie is determined using the parent folders name.
 */

// xbmc artwork/nfo utility
include("fn:lib/xbmc")


args.eachMediaFolder { dir ->
	// fetch only missing artwork by default
	if (_args.conflict == "skip" && dir.hasFile{it =~ /movie.nfo$/} && dir.hasFile{it =~ /folder.jpg$/} && dir.hasFile{it =~ /backdrop.jpg$/}) {
		println "Skipping $dir"
		return
	}
	
	def videos = dir.listFiles{ it.isVideo() }
	
	def query = _args.query ?: dir.name
	def options = TheMovieDB.searchMovie(query, _args.locale)
	if (options.isEmpty()) {
		println "Movie not found: $query"
		return
	}
	
	// sort by relevance
	options = options.sortBySimilarity(query, { it.name })
	
	// auto-select movie
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
