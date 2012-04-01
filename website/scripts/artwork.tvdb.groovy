// filebot -script "http://filebot.sf.net/scripts/artwork.tvdb.groovy" -trust-script /path/to/media/

/*
 * Fetch series and season banners for all tv shows. Series name is auto-detected if possible or the folder name is used.
 */

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


def fetchNfo(outputFile, series) {
	def info = TheTVDB.getSeriesInfo(series, _args.locale)
	println info  
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
	
	TheTVDB.getBannerList(series).each {
		println "Available banner: $it.url => $it"
	}
	
	// fetch nfo
	fetchNfo(seriesDir['tvshow.nfo'], series)
			
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


args.eachMediaFolder { dir ->
	// fetch only missing artwork by default
	if (_args.conflict == "skip" && dir.hasFile{it =~ /tvshow.nfo$/} && dir.hasFile{it =~ /folder.jpg$/} && dir.hasFile{it =~ /banner.jpg$/}) {
		println "Skipping $dir"
		return
	}
	
	def videos = dir.listFiles{ it.isVideo() }
	
	def query = _args.query ?: detectSeriesName(videos, _args.locale)
	def sxe = videos.findResult{ parseEpisodeNumber(it) }
	
	if (query == null) {
		query = dir.dir.hasFile{ it.name =~ /Season/ && it.isDirectory() } ? dir.dir.name : dir.name
	}
	
	println "$dir => Search by $query"
	def options = TheTVDB.search(query, _args.locale)
	if (options.isEmpty()) {
		println "TV Series not found: $query"
		return
	}
	
	// sort by relevance
	options = options.sortBySimilarity(query, { it.name })
	
	// auto-select series
	def series = options[0]
	
	// maybe require user input
	if (options.size() != 1 && !_args.nonStrict && !java.awt.GraphicsEnvironment.headless) {
		series = javax.swing.JOptionPane.showInputDialog(null, "Please select TV Show:", dir.path, 3, null, options.toArray(), series);
		if (series == null) return
	}
	
	// auto-detect structure
	def seriesDir = [dir.dir, dir].sortBySimilarity(series.name, { it.name })[0]
	def season = sxe && sxe.season > 0 ? sxe.season : 1
	
	println "$dir => $series"
	fetchSeriesBannersAndNfo(seriesDir, dir, series, season)
}
