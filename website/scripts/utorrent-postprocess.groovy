// filebot -script "http://filebot.sf.net/scripts/utorrent-postprocess.groovy" --output "X:/media" --action copy --conflict override -non-strict -trust-script -Xxbmc=localhost "-Xut_dir=%D" "-Xut_file=%F" "-Xut_label=%L" "-Xut_state=%S" "-Xut_kind=%K"
def input = []

// print input parameters
_args.parameters.each{ k, v -> println "Parameter: $k = $v" }

if (ut_kind == "multi") {
	input += new File(ut_dir).getFiles() // multi-file torrent
} else {
	input += new File(ut_dir, ut_file) // single-file torrent
}

// extract archives if necessary
input += extract(file:input)

// process only media files
input = input.findAll{ it.isVideo() || it.isSubtitle() }

// print input fileset
input.each{ println "Input: $it" }

// group episodes/movies and rename according to XBMC standards
def groups = input.groupBy{
	def tvs = detectSeriesName(it)
	def mov = detectMovie(it, false)
	println "$it.name [series: $tvs, movie: $mov]"
	
	// DECIDE EPISODE VS MOVIE (IF NOT CLEAR)
	if (tvs && mov) {
		if (it.name =~ "(?i:$tvs - .+)" || parseEpisodeNumber(it.name) || parseDate(it.name)) {
			println "Exclude Movie: $mov"
			mov = null
		} else if (detectMovie(it, true)) {
			println "Exclude Series: $tvs"
			tvs = null
		}
	}
	return [tvs:tvs, mov:mov]
}

groups.each{ group, files ->
	// EPISODE MODE
	if (group.tvs && !group.mov) {
		def output = rename(file:files, format:'TV Shows/{n}{episode.special ? "/Special" : "/Season "+s}/{n} - {episode.special ? "S00E"+special.pad(2) : s00e00} - {t}', db:'TheTVDB')
		
		output*.dir.unique().each{ dir ->
			println "Fetching artwork for $dir from TheTVDB"
			def query = group.tvs
			def sxe = output.findResult{ parseEpisodeNumber(it) }
			def options = TheTVDB.search(query, _args.locale)
			if (options.isEmpty()) {
				println "TV Series not found: $query"
				return
			}
			options = options.sortBySimilarity(query, { it.name })
			def series = options[0]
			def seriesDir = [dir.dir, dir].sortBySimilarity(series.name, { it.name })[0]
			def season = sxe && sxe.season > 0 ? sxe.season : 1
			fetchSeriesBannersAndNfo(seriesDir, dir, series, season)
		}
	}
	
	// MOVIE MODE
	if (group.mov && !group.tvs) {
		def output = rename(file:files, format:'Movies/{n} ({y})/{n} ({y}){" CD$pi"}', db:'TheMovieDB')
		
		output*.dir.unique().each{ dir ->
			println "Fetching artwork for $dir from TheMovieDB"
			try {
				fetchMovieArtworkAndNfo(dir, group.mov)
			} catch(e) {
				println "${e.class.simpleName}: ${e.message}"
			}
		}
	}
}



// make XBMC scan for new content
try {
	xbmc.split(/[\s,|]+/).each{
		println "Notify XBMC: $it"
		telnet(it, 9090) { writer, reader ->
			def msg = '{"id":1,"method":"VideoLibrary.Scan","params":[],"jsonrpc":"2.0"}'
			writer.println(msg)
		}
	}
} catch(e) {
	println "${e.class.simpleName}: ${e.message}"
}
// END OF SCRIPT




// FUNCTIONS for TMDB and TVDB artwork/nfo
def fetchBanner(outputFile, series, bannerType, bannerType2 = null, season = null) {
	// select and fetch banner
	def banner = [_args.locale.language, null].findResult { TheTVDB.getBanner(series, [BannerType:bannerType, BannerType2:bannerType2, Season:season, Language:it]) }
	if (banner == null) {
		println "Banner not found: $outputFile / $bannerType:$bannerType2"
		return null
	}
	println "Fetching $outputFile => $banner"
	return banner.url.saveAs(outputFile)
}
def fetchSeriesNfo(outputFile, series) {
	def info = TheTVDB.getSeriesInfo(series, _args.locale)
	info.applyXmlTemplate('''<tvshow xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
			<title>$name</title>
			<year>$firstAired.year</year>
			<top250></top250>
			<seasons>-1</seasons>
			<episode></episode>
			<episodeguideurl></episodeguideurl>
			<displayseason>-1</displayseason>
			<displayepisode>-1</displayepisode>
			<rating>$rating</rating>
			<votes>$ratingCount</votes>
			<outline></outline>
			<plot>$overview</plot>
			<tagline></tagline>
			<runtime>$runtime</runtime>
			<mpaa>$contentRating</mpaa>
			<playcount></playcount>
			<lastplayed></lastplayed>
			<id>$id</id>
			<episodeguide><url cache="${id}.xml">http://www.thetvdb.com/api/1D62F2F90030C444/series/${id}/all/''' + _args.locale.language + '''.zip</url></episodeguide>
			<genre>${!genres.empty ? genres[0] : ''}</genre>
			<set></set>
			<credits></credits>
			<director></director>
			<thumb>$bannerUrl</thumb>
			<premiered>$firstAired</premiered>
			<status>$status</status>
			<studio>$network</studio>
			<trailer></trailer>
			<gsp:scriptlet> actors.each { </gsp:scriptlet>
				<actor>
					<name>$it</name>
					<role></role>
				</actor>
			<gsp:scriptlet> } </gsp:scriptlet>
			<artist></artist>
		</tvshow>
	''')
	.replaceAll(/\t|\r|\n/, '') // xbmc can't handle leading/trailing whitespace properly
	.saveAs(outputFile)
}
def fetchSeriesBannersAndNfo(seriesDir, seasonDir, series, season) {
	println "Fetch nfo and banners for $series / Season $season"
	// fetch nfo
	fetchSeriesNfo(seriesDir['tvshow.nfo'], series)
	// fetch series banner, fanart, posters, etc
	["680x1000",  null].findResult{ fetchBanner(seriesDir['folder.jpg'], series, "poster", it) }
	["graphical", null].findResult{ fetchBanner(seriesDir['banner.jpg'], series, "series", it) }
	// fetch highest resolution fanart
	["1920x1080", "1280x720", null].findResult{ fetchBanner(seriesDir["fanart.jpg"], series, "fanart", it) }
	// fetch season banners
	if (seasonDir != seriesDir) {
		fetchBanner(seasonDir["folder.jpg"], series, "season", "season", season)
		fetchBanner(seasonDir["banner.jpg"], series, "season", "seasonwide", season)
	}
}

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
def fetchMovieNfo(outputFile, movieInfo) {
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
	''')
	.replaceAll(/\t|\r|\n/, '') // xbmc can't handle leading/trailing whitespace properly
	.saveAs(outputFile)
}
def fetchMovieArtworkAndNfo(movieDir, movie) {
	println "Fetch nfo and artwork for $movie"
	def movieInfo = TheMovieDB.getMovieInfo(movie, Locale.ENGLISH)
	// fetch nfo
	fetchMovieNfo(movieDir['movie.nfo'], movieInfo)
	// fetch series banner, fanart, posters, etc
	fetchArtwork(movieDir['folder.jpg'], movieInfo, 'poster', 'original')
	fetchArtwork(movieDir['backdrop.jpg'], movieInfo, 'backdrop', 'original')
}
