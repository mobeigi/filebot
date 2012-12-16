
import static net.sourceforge.filebot.WebServices.*

import groovy.xml.*
import net.sourceforge.filebot.mediainfo.*



/**
 * XBMC helper functions
 */
def invokeScanVideoLibrary(host, port = 9090) {
	_guarded {
		telnet(host, port) { writer, reader ->
			writer.println('{"id":1,"method":"VideoLibrary.Scan","params":[],"jsonrpc":"2.0"}') // API call for latest XBMC release
		}
	}
}



/**
 * Plex helpers
 */
def refreshPlexLibrary(server, port = 32400) {
	_guarded {
		new URL("http://$server:$port/library/sections/all/refresh").get()
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
	info.applyXml('''<tvshow xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
			<title>$name</title>
			<sorttitle>${[name, firstAired as String].findAll{ !it.empty }.join(/ :: /)}</sorttitle>
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
	_guarded {
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

def fetchAllMovieArtwork(outputFolder, movieInfo, category, language) {
	// select and fetch artwork
	def artwork = TheMovieDB.getArtwork(movieInfo.id as String)
	def selection = [language, 'en', null].findResults{ l -> artwork.findAll{ (l == it.language || l == null) && it.category == category } }.flatten().findAll{ it?.url }.unique()
	if (selection == null) {
		println "Artwork not found: $outputFolder"
		return null
	}
	selection.eachWithIndex{ s, i ->
		def outputFile = new File(outputFolder, "$category-${(i+1).pad(2)}.jpg")
		println "Fetching $outputFile => $s"
		s.url.saveAs(outputFile)
	}
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

def createFileInfoXml(file) {
	_guarded {
		def mi = MediaInfo.snapshot(file)
		def out = new StringWriter()
		def xml = new MarkupBuilder(out)
		xml.fileinfo() {
			streamdetails() {
				mi.each { kind, streams ->
					def section = kind.toString().toLowerCase()
					streams.each { s ->
						if (section == 'video') {
							video() {
								codec((s.'Encoded_Library/Name' ?: s.'CodecID/Hint' ?: s.'Format').replaceAll(/[ ].+/, '').trim())
								aspect(s.'DisplayAspectRatio')
								width(s.'Width')
								height(s.'Height')
							}
						}
						if (section == 'audio') {
							audio() {
								codec((s.'CodecID/Hint' ?: s.'Format').replaceAll(/\p{Punct}/, '').trim())
								language(s.'Language/String3')
								channels(s.'Channel(s)')
							}
						}
						if (section == 'text') {
							subtitle() {
								language(s.'Language/String3')
							}
						}
					}
				}
			}
		}
		return out.toString()
	}
}

def fetchMovieNfo(outputFile, movieInfo, movieFile) {
	movieInfo.applyXml('''<movie xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
			<title>$name</title>
			<originaltitle>$originalName</originaltitle>
			<sorttitle>${[collection, name, released as String].findAll{ !it.empty }.join(/ :: /)}</sorttitle>
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
			''' + ((movieFile != null ? createFileInfoXml(movieFile) : null) ?: '') + '''
			<imdb id='tt${imdbId.pad(7)}'>http://www.imdb.com/title/tt${imdbId.pad(7)}/</imdb>
			<tmdb id='$id'>http://www.themoviedb.org/movie/$id</tmdb>
		</movie>
	''')
	.replaceAll(/\t|\r|\n/, '') // xbmc can't handle leading/trailing whitespace properly
	.saveAs(outputFile)
}

def fetchMovieArtworkAndNfo(movieDir, movie, movieFile = null, fetchAll = false, locale = _args.locale) {
	_guarded {
		def movieInfo = TheMovieDB.getMovieInfo(movie, locale)
		
		// fetch nfo
		fetchMovieNfo(movieDir['movie.nfo'], movieInfo, movieFile)
		
		// fetch series banner, fanart, posters, etc
		fetchMovieArtwork(movieDir['poster.jpg'], movieInfo, 'posters', locale.language)
		fetchMovieArtwork(movieDir['fanart.jpg'], movieInfo, 'backdrops', locale.language)
		
		fetchMovieFanart(movieDir['clearart.png'], movieInfo, 'movieart', null, locale)
		fetchMovieFanart(movieDir['logo.png'], movieInfo, 'movielogo', null, locale)
		['bluray', 'dvd', null].findResult { diskType -> fetchMovieFanart(movieDir['disc.png'], movieInfo, 'moviedisc', diskType, locale) }
		
		if (fetchAll) {
			fetchAllMovieArtwork(movieDir['backdrops'], movieInfo, 'backdrops', locale.language)
		}
	}
}
