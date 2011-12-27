// filebot -script "http://filebot.sf.net/scripts/artwork.tvdb.groovy" -trust-script /path/to/media/

// EXPERIMENTAL // HERE THERE BE DRAGONS
if (net.sourceforge.filebot.Settings.applicationRevisionNumber < 802) throw new Exception("Application revision too old")


/*
 * Fetch series and season banners for all tv shows
 */

def fetchBanner(outputFile, series, bannerType, bannerType2, season = null) {
	// select and fetch banner
	def banner = TheTVDB.getBanner(series, bannerType, bannerType2, season, Locale.ENGLISH, 0)
	if (banner == null) {
		println "Banner not found: $outputFile"
		return null
	}
	
	println "Fetching $outputFile => $banner"
	return banner.url.saveAs(outputFile)
}


def fetchNfo(outputFile, series) {
	TheTVDB.getSeriesInfo(series, Locale.ENGLISH).applyXmlTemplate('''<tvshow xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
			<title>$name</title>
			<year>${firstAired?.year}</year>
			<rating>$rating</rating>
			<votes>$ratingCount</votes>
			<plot>$overview</plot>
			<runtime>$runtime</runtime>
			<mpaa>$contentRating</mpaa>
			<genre>${genre.size() > 0 ? genre.get(0) : ''}</genre>
			<id>$id</id>
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
	''').saveAs(outputFile)
}


def fetchSeriesBannersAndNfo(seriesDir, seasonDir, series, season) {
	println "Fetch nfo and banners for $series / Season $season"
	
	// fetch nfo
	fetchNfo(seriesDir['tvshow.nfo'], series)
		
	// fetch series banner, fanart, posters, etc
	fetchBanner(seriesDir['folder.jpg'], series, "poster", "680x1000")
	fetchBanner(seriesDir['banner.jpg'], series, "series", "graphical")
	
	// fetch highest resolution fanart
	["1920x1080", "1280x720"].findResult{ fetchBanner(seriesDir["fanart.jpg"], series, "fanart", it) }
	
	// fetch season banners
	if (seasonDir != seriesDir) {
		fetchBanner(seasonDir["folder.jpg"], series, "season", "season", season)
		fetchBanner(seasonDir["banner.jpg"], series, "season", "seasonwide", season)
	}
}


def jobs = args.getFolders().findResults { dir ->
	def videos = dir.listFiles{ it.isVideo() }
	if (videos.isEmpty()) {
		return null
	}
	
	def query = _args.query ?: detectSeriesName(videos)
	def sxe = videos.findResult{ parseEpisodeNumber(it) }
	
	if (query == null) {
		query = dir.dir.hasFile{ it.name =~ /Season/ && it.isDirectory() } ? dir.dir.name : dir.name
		println "Failed to detect series name from video files -> Query by $query instead"
	}
	
	def options = TheTVDB.search(query, Locale.ENGLISH)
	if (options.isEmpty()) {
		println "TV Series not found: $query"
		return null
	}
	
	// auto-select series
	def series = options.sortBySimilarity(query, { it.name })[0]
	
	// auto-detect structure
	def seriesDir = [dir.dir, dir].sortBySimilarity(series.name, { it.name })[0]
	def season = sxe && sxe.season > 0 ? sxe.season : 1
	
	return { fetchSeriesBannersAndNfo(seriesDir, dir, series, season) }
}

parallel(jobs, 10)
