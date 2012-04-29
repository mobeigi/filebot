// xbmc functions
def invokeScanVideoLibrary(host, port = 9090) {
	try {
		telnet(host, 9090) { writer, reader ->
			writer.println('{"id":1,"method":"VideoLibrary.Scan","params":[],"jsonrpc":"2.0"}') // API call for latest XBMC release
		}
		return true
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
		return false
	}
}


// functions for TheTVDB artwork/nfo
def fetchSeriesBanner(outputFile, series, bannerType, bannerType2, season, locale) {
	// select and fetch banner
	def banner = [locale, null].findResult { TheTVDB.getBanner(series, [BannerType:bannerType, BannerType2:bannerType2, Season:season, Language:it]) }
	if (banner == null) {
		println "Banner not found: $outputFile / $bannerType:$bannerType2"
		return null
	}
	println "Fetching $outputFile => $banner"
	return banner.url.saveAs(outputFile)
}

def fetchSeriesNfo(outputFile, series, locale) {
	def info = TheTVDB.getSeriesInfo(series, locale)
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
			<episodeguide><url cache="${id}.xml">http://www.thetvdb.com/api/1D62F2F90030C444/series/${id}/all/''' + locale.language + '''.zip</url></episodeguide>
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

def fetchSeriesArtworkAndNfo(seriesDir, seasonDir, series, season, locale = _args.locale) {
	try {
		// fetch nfo
		fetchSeriesNfo(seriesDir['tvshow.nfo'], series, locale)
		
		// fetch series banner, fanart, posters, etc
		["680x1000",  null].findResult{ fetchSeriesBanner(seriesDir['folder.jpg'], series, "poster", it, null, locale) }
		["graphical", null].findResult{ fetchSeriesBanner(seriesDir['banner.jpg'], series, "series", it, null, locale) }
		
		// fetch highest resolution fanart
		["1920x1080", "1280x720", null].findResult{ fetchSeriesBanner(seriesDir["fanart.jpg"], series, "fanart", it, null, locale) }
		
		// fetch season banners
		if (seasonDir != seriesDir) {
			fetchSeriesBanner(seasonDir["folder.jpg"], series, "season", "season", season, locale)
			fetchSeriesBanner(seasonDir["banner.jpg"], series, "season", "seasonwide", season, locale)
		}
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
	}
}



// functions for TheMovieDB artwork/nfo
def fetchMovieArtwork(outputFile, movieInfo, artworkType, artworkSize) {
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

def fetchMovieArtworkAndNfo(movieDir, movie, locale = _args.locale) {
	try {
		def movieInfo = TheMovieDB.getMovieInfo(movie, locale)
		// fetch nfo
		fetchMovieNfo(movieDir['movie.nfo'], movieInfo)
		
		// fetch series banner, fanart, posters, etc
		fetchMovieArtwork(movieDir['folder.jpg'], movieInfo, 'poster', 'original')
		fetchMovieArtwork(movieDir['backdrop.jpg'], movieInfo, 'backdrop', 'original')
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
	}
}
