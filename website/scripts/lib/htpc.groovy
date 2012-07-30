
import static net.sourceforge.filebot.WebServices.*


/**
 * XBMC helper functions
 */
def invokeScanVideoLibrary(host, port = 9090) {
	try {
		telnet(host, port) { writer, reader ->
			writer.println('{"id":1,"method":"VideoLibrary.Scan","params":[],"jsonrpc":"2.0"}') // API call for latest XBMC release
		}
		return true
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
		return false
	}
}



/**
 * Plex helpers
 */
def refreshPlexLibrary(server, port = 32400, files = null) {
	try {
		def sections = new URL("http://$server:$port/plex").getXml()
		def locations = sections.Directory.Location.collect{ [path:it.'@path', key:it.parent().'@key'] }
		
		// limit refresh locations
		if (files != null) {
			locations = locations.findAll{ loc -> files.find{ it.path; it.path.startsWith(loc.path) }}
		}
		
		locations*.key.unique().each{ key ->
			new URL("http://$server:$port/library/sections/$key/refresh/").get()
		}
		return true
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
		return false
	}
}



/**
 * TheTVDB artwork/nfo helpers
 */
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

def fetchSeriesFanart(outputFile, series, type, season, locale) {
	def fanart = [locale, null].findResult{ lang -> FanartTV.getSeriesArtwork(series.seriesId).find{ type == it.type && (season == null || season == it.season) && (lang == null || lang == it.language) }}
	if (fanart == null) {
		println "Fanart not found: $outputFile / $type"
		return null
	}
	println "Fetching $outputFile => $fanart"
	return fanart.url.saveAs(outputFile)
}

def fetchSeriesNfo(outputFile, series, locale) {
	def info = TheTVDB.getSeriesInfo(series, locale)
	info.applyXmlTemplate('''<tvshow xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
			<title>$name</title>
			<year>$firstAired.year</year>
			<rating>$rating</rating>
			<votes>$ratingCount</votes>
			<plot>$overview</plot>
			<runtime>$runtime</runtime>
			<mpaa>$contentRating</mpaa>
			<id>$id</id>
			<episodeguide><url cache="${id}.xml">http://www.thetvdb.com/api/1D62F2F90030C444/series/${id}/all/''' + locale.language + '''.zip</url></episodeguide>
			<genre>${!genres.empty ? genres[0] : ''}</genre>
			<thumb>$bannerUrl</thumb>
			<premiered>$firstAired</premiered>
			<status>$status</status>
			<studio>$network</studio>
			<gsp:scriptlet> actors.each { </gsp:scriptlet>
				<actor>
					<name>$it</name>
				</actor>
			<gsp:scriptlet> } </gsp:scriptlet>
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
		["680x1000",  null].findResult{ fetchSeriesBanner(seriesDir['poster.jpg'], series, "poster", it, null, locale) }
		["graphical", null].findResult{ fetchSeriesBanner(seriesDir['banner.jpg'], series, "series", it, null, locale) }
		
		// fetch highest resolution fanart
		["1920x1080", "1280x720", null].findResult{ fetchSeriesBanner(seriesDir["fanart.jpg"], series, "fanart", it, null, locale) }
		
		// fetch season banners
		if (seasonDir != seriesDir) {
			fetchSeriesBanner(seasonDir["poster.jpg"], series, "season", "season", season, locale)
			fetchSeriesBanner(seasonDir["banner.jpg"], series, "season", "seasonwide", season, locale)
		}
		
		// fetch fanart
		fetchSeriesFanart(seriesDir['clearart.png'], series, 'clearart', null, locale)
		fetchSeriesFanart(seriesDir['logo.png'], series, 'clearlogo', null, locale)
		fetchSeriesFanart(seriesDir['landscape.jpg'], series, 'tvthumb', null, locale)
		
		// fetch season fanart
		if (seasonDir != seriesDir) {
			fetchSeriesFanart(seasonDir['landscape.jpg'], series, 'seasonthumb', season, locale)
		}
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
	}
}



/**
 * TheMovieDB artwork/nfo helpers
 */
def fetchMovieArtwork(outputFile, movieInfo, category, language) {
	// select and fetch artwork
	def artwork = TheMovieDB.getArtwork(movieInfo.id as String)
	def selection = [language, 'en', null].findResult{ l -> artwork.find{ (l == it.language || l == null) && it.category == category } }
	if (selection == null) {
		println "Artwork not found: $outputFile"
		return null
	}
	println "Fetching $outputFile => $selection"
	return selection.url.saveAs(outputFile)
}

def fetchMovieFanart(outputFile, movieInfo, type, diskType, locale) {
	def fanart = [locale, null].findResult{ lang -> FanartTV.getMovieArtwork(movieInfo.id).find{ type == it.type && (diskType == null || diskType == it.diskType) && (lang == null || lang == it.language) }}
	if (fanart == null) {
		println "Fanart not found: $outputFile / $type"
		return null
	}
	println "Fetching $outputFile => $fanart"
	return fanart.url.saveAs(outputFile)
}

def fetchMovieNfo(outputFile, movieInfo) {
	movieInfo.applyXmlTemplate('''<movie xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
			<title>$name</title>
			<originaltitle>$originalName</originaltitle>
			<set>$collection</set>
			<year>$released.year</year>
			<rating>$rating</rating>
			<votes>$votes</votes>
			<mpaa>$certification</mpaa>
			<id>tt${imdbId.pad(7)}</id>
			<plot>$overview</plot>
			<tagline>$tagline</tagline>
			<runtime>$runtime</runtime>
			<genre>${!genres.empty ? genres[0] : ''}</genre>
			<director>$director</director>
			<gsp:scriptlet> cast.each { </gsp:scriptlet>
				<actor>
					<name>${it?.name}</name>
					<role>${it?.character}</role>
				</actor>
			<gsp:scriptlet> } </gsp:scriptlet>
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
		fetchMovieArtwork(movieDir['poster.jpg'], movieInfo, 'posters', locale.language)
		fetchMovieArtwork(movieDir['fanart.jpg'], movieInfo, 'backdrops', locale.language)
		
		fetchMovieFanart(movieDir['clearart.png'], movieInfo, 'movieart', null, locale)
		fetchMovieFanart(movieDir['logo.png'], movieInfo, 'movielogo', null, locale)
		['bluray', 'dvd', null].findResult { diskType -> fetchMovieFanart(movieDir['disc.png'], movieInfo, 'moviedisc', diskType, locale) }
	} catch(e) {
		println "${e.class.simpleName}: ${e.message}"
	}
}
