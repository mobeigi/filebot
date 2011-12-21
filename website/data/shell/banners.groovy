// filebot -script "http://filebot.sourceforge.net/data/shell/banners.groovy" -trust-script /path/to/media/

/*
 * Fetch series and season banners for all tv shows
 */
import static net.sourceforge.filebot.WebServices.*


def fetchBanner(outputDir, outputName, series, bannerType, bannerType2, season = null) {
	// select and fetch banner
	def banner = TheTVDB.getBanner(series, bannerType, bannerType2, season, Locale.ENGLISH, 0)
	if (banner == null) {
		println "Banner not found: $outputName"
		return null
	}
	
	println "Fetching banner $banner"
	return banner.url.saveAs(new File(outputDir, outputName + ".jpg"))
}


def fetchNfo(outputDir, outputName, series) {
	def info = TheTVDB.getSeriesInfo(series, Locale.ENGLISH)
	println "Writing nfo $info"
	
	new File(outputDir, outputName + ".nfo").withWriter{ out ->
		out.println("Name: $info.name")
		out.println("IMDb: http://www.imdb.com/title/tt${info.imdbId.pad(7)}")
		out.println("Actors: ${info.actors.join(', ')}")
		out.println("Genere: ${info.genre.join(', ')}")
		out.println("Language: ${info.language.displayName}")
		out.println("Overview: $info.overview")
	}
}


def fetchSeriesBannersAndNfo(dir, series, seasons) {
	println "Fetch nfo and banners for $series / Season $seasons"
	
	// fetch nfo
	fetchNfo(dir, series.name, series)
	
	// fetch series banner, fanart, posters, etc
	fetchBanner(dir, "folder", series, "poster", "680x1000")
	fetchBanner(dir, "banner", series, "series", "graphical")
	
	// fetch highest resolution fanart
	["1920x1080", "1280x720"].findResult{ bannerType2 -> fetchBanner(dir, "fanart", series, "fanart", bannerType2) }
	
	// fetch season banners
	seasons.each { s ->
		fetchBanner(dir, "folder-S${s.pad(2)}", series, "season", "season", s)
		fetchBanner(dir, "banner-S${s.pad(2)}", series, "season", "seasonwide", s)
	}
}


args.eachMediaFolder() { dir ->
	println "Processing $dir"
	def videoFiles = dir.listFiles{ it.isVideo() }
	
	def seriesName = detectSeriesName(videoFiles)
	def seasons = videoFiles.findResults { guessEpisodeNumber(it)?.season }.unique()
	
	if (seriesName == null) {
		println "Failed to detect series name from files -> Query by ${dir.name} instead"
		seriesName = dir.name
	}
	
	def options = TheTVDB.search(seriesName)
	if (options.isEmpty()) {
		println "TV Series not found: $seriesName"
		return;
	}
	
	fetchSeriesBannersAndNfo(dir, options[0], seasons)
}
