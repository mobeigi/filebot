// filebot -script "http://filebot.sourceforge.net/data/shell/banners.groovy" -trust-script /path/to/media/

/*
 * Fetch series and season banners for all tv shows
 */
import static net.sourceforge.filebot.WebServices.*


def fetchBanner(dir, series, bannerType, bannerType2, season = null) {
	def name = "$series $bannerType ${season ? 'S'+season : 'all'} $bannerType2".space('.')
	
	// select and fetch banner
	def banner = TheTVDB.getBanner(series, bannerType, bannerType2, season, Locale.ENGLISH)
	if (banner == null) {
		println "Banner not found: $name"
		return;
	}
	
	println "Fetching $name"
	banner.url.saveAs(new File(dir, name + "." + banner.extension))
}


def fetchSeriesBanners(dir, series, seasons) {
	println "Fetch banners for $series / Season $seasons"
	
	// fetch series banner
	fetchBanner(dir, series, "series", "graphical")
	
	// fetch season banners
	seasons.each { s ->
		fetchBanner(dir, series, "season", "season", s)
		fetchBanner(dir, series, "season", "seasonwide", s)
	}
}


args.eachMediaFolder() { dir ->
	println "Processing $dir"
	def videoFiles = dir.listFiles{ it.isVideo() }
	
	def seriesName = detectSeriesName(videoFiles)
	def seasons = videoFiles.findResults { guessEpisodeNumber(it)?.season }.unique()
	
	def options = TheTVDB.search(seriesName)
	if (options.isEmpty()) {
		println "TV Series not found: $name"
		return;
	}
	
	fetchSeriesBanners(dir, options[0], seasons)
}
