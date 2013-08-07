import static net.sourceforge.filebot.WebServices.*
import static groovy.json.StringEscapeUtils.*

import groovy.xml.*
import net.sourceforge.filebot.mediainfo.*


/**
 * XBMC helper functions
 */
def scanVideoLibrary(host, port) {
	_guarded {
		telnet(host, port) { writer, reader ->
			writer.println("""{"jsonrpc":"2.0","method":"VideoLibrary.Scan","id":1}""")
		}
	}
}

def showNotification(host, port, title, message, image) {
	_guarded {
		telnet(host, port) { writer, reader ->
			writer.println("""{"jsonrpc":"2.0","method":"GUI.ShowNotification","params":{"title":"${escapeJavaScript(title)}","message":"${escapeJavaScript(message)}", "image":"${escapeJavaScript(image)}"},"id":1}""")
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
def fetchSeriesBanner(outputFile, series, bannerType, bannerType2, season, override, locale) {
	if (outputFile.exists() && !override) {
		_log.finest "Banner already exists: $outputFile"
		return outputFile
	}
	
	// select and fetch banner
	def banner = [locale, null].findResult { TheTVDB.getBanner(series, [BannerType:bannerType, BannerType2:bannerType2, Season:season, Language:it]) }
	if (banner == null) {
		_log.finest "Banner not found: $outputFile / $bannerType:$bannerType2"
		return null
	}
	_log.finest "Fetching $outputFile => $banner"
	return banner.url.saveAs(outputFile)
}

def fetchSeriesFanart(outputFile, series, type, season, override, locale) {
	if (outputFile.exists() && !override) {
		_log.finest "Fanart already exists: $outputFile"
		return outputFile
	}
	
	def fanart = [locale, null].findResult{ lang -> FanartTV.getSeriesArtwork(series.seriesId).find{ type == it.type && (season == null || season == it.season) && (lang == null || lang == it.language) }}
	if (fanart == null) {
		_log.finest "Fanart not found: $outputFile / $type"
		return null
	}
	_log.finest "Fetching $outputFile => $fanart"
	return fanart.url.saveAs(outputFile)
}

def fetchSeriesNfo(outputFile, seriesInfo, override, locale) {
	def i = seriesInfo
	XML {
		tvshow {
			title(i.name)
			sorttitle([i.name, i.firstAired as String].findAll{ it?.length() > 0 }.join('::'))
			year(i.firstAired?.year)
			rating(i.rating)
			votes(i.ratingCount)
			plot(i.overview)
			runtime(i.runtime)
			mpaa(i.contentRating)
			id(i.id)
			episodeguide {
				url(cache:"${i.id}.xml", "http://www.thetvdb.com/api/1D62F2F90030C444/series/${i.id}/all/${locale.language}.zip")
			}
			genre(i.genres?.size() > 0 ? i.genres[0] : null)
			thumb(i.bannerUrl)
			premiered(i.firstAired)
			status(i.status)
			studio(i.network)
			i.actors?.each{ n ->
				actor {
					name(n)
				}
			}
			tvdb(id:i.id, "http://www.thetvdb.com/?tab=series&id=${i.id}")
		}
	}
	.saveAs(outputFile)
}

def fetchSeriesArtworkAndNfo(seriesDir, seasonDir, series, season, override = false, locale = _args.locale) {
	_guarded {
		// fetch nfo
		def seriesInfo = TheTVDB.getSeriesInfo(series, locale)
		fetchSeriesNfo(seriesDir['tvshow.nfo'], seriesInfo, override, locale)
		
		// fetch series banner, fanart, posters, etc
		["680x1000",  null].findResult{ fetchSeriesBanner(seriesDir['poster.jpg'], series, "poster", it, null, override, locale) }
		["graphical", null].findResult{ fetchSeriesBanner(seriesDir['banner.jpg'], series, "series", it, null, override, locale) }
		
		// fetch highest resolution fanart
		["1920x1080", "1280x720", null].findResult{ fetchSeriesBanner(seriesDir["fanart.jpg"], series, "fanart", it, null, override, locale) }
		
		// fetch season banners
		if (seasonDir != seriesDir) {
			fetchSeriesBanner(seasonDir["poster.jpg"], series, "season", "season", season, override, locale)
			fetchSeriesBanner(seasonDir["banner.jpg"], series, "season", "seasonwide", season, override, locale)
		}
		
		// fetch fanart
		fetchSeriesFanart(seriesDir['clearart.png'], series, 'clearart', null, override, locale)
		fetchSeriesFanart(seriesDir['logo.png'], series, 'clearlogo', null, override, locale)
		fetchSeriesFanart(seriesDir['landscape.jpg'], series, 'tvthumb', null, override, locale)
		
		// fetch season fanart
		if (seasonDir != seriesDir) {
			fetchSeriesFanart(seasonDir['landscape.jpg'], series, 'seasonthumb', season, override, locale)
		}
	}
}



/**
 * TheMovieDB artwork/nfo helpers
 */
def fetchMovieArtwork(outputFile, movieInfo, category, override, locale) {
	if (outputFile.exists() && !override) {
		_log.finest "Artwork already exists: $outputFile"
		return outputFile
	}
	
	// select and fetch artwork
	def artwork = TheMovieDB.getArtwork(movieInfo.id as String)
	def selection = [locale.language, 'en', null].findResult{ l -> artwork.find{ (l == it.language || l == null) && it.category == category } }
	if (selection == null) {
		_log.finest "Artwork not found: $outputFile"
		return null
	}
	_log.finest "Fetching $outputFile => $selection"
	return selection.url.saveAs(outputFile)
}

def fetchAllMovieArtwork(outputFolder, movieInfo, category, override, locale) {
	// select and fetch artwork
	def artwork = TheMovieDB.getArtwork(movieInfo.id as String)
	def selection = [locale.language, 'en', null].findResults{ l -> artwork.findAll{ (l == it.language || l == null) && it.category == category } }.flatten().findAll{ it?.url }.unique()
	if (selection == null) {
		_log.finest "Artwork not found: $outputFolder"
		return null
	}
	selection.eachWithIndex{ s, i ->
		def outputFile = new File(outputFolder, "$category-${(i+1).pad(2)}.jpg")
		if (outputFile.exists() && !override) {
			_log.finest "Artwork already exists: $outputFile"
		} else {
			_log.finest "Fetching $outputFile => $s"
			s.url.saveAs(outputFile)
		}
	}
}

def fetchMovieFanart(outputFile, movieInfo, type, diskType, override, locale) {
	if (outputFile.exists() && !override) {
		_log.finest "Fanart already exists: $outputFile"
		return outputFile
	}
	
	def fanart = [locale, null].findResult{ lang -> FanartTV.getMovieArtwork(movieInfo.id).find{ type == it.type && (diskType == null || diskType == it.diskType) && (lang == null || lang == it.language) }}
	if (fanart == null) {
		_log.finest "Fanart not found: $outputFile / $type"
		return null
	}
	_log.finest "Fetching $outputFile => $fanart"
	return fanart.url.saveAs(outputFile)
}

def fetchMovieNfo(outputFile, movieInfo, movieFile, override) {
	def i = movieInfo
	def mi = _guarded{ movieFile ? MediaInfo.snapshot(movieFile) : null }
	XML {
		movie {
			title(i.name)
			originaltitle(i.originalName)
			sorttitle([i.collection, i.name, i.released as String].findAll{ it?.length() > 0 }.join('::'))
			set(i.collection)
			year(i.released?.year)
			rating(i.rating)
			votes(i.votes)
			mpaa(i.certification)
			id("tt" + (i.imdbId ?: 0).pad(7))
			plot(i.overview)
			tagline(i.tagline)
			runtime(i.runtime)
			genre(i.genres?.size() > 0 ? i.genres[0] : null)
			director(i.director)
			i.cast?.each{ a ->
				actor {
					name(a.name)
					role(a.character)
				}
			}
			i.trailers?.each{ t ->
				t.sources.each { s, v ->
					trailer(type:t.type, name:t.name, size:s, v)
				}
			}
			fileinfo {
				streamdetails {
					mi?.each { kind, streams ->
						def section = kind.toString().toLowerCase()
						streams.each { s ->
							if (section == 'video') {
								video {
									codec((s.'Encoded_Library/Name' ?: s.'CodecID/Hint' ?: s.'Format').replaceAll(/[ ].+/, '').trim())
									aspect(s.'DisplayAspectRatio')
									width(s.'Width')
									height(s.'Height')
								}
							}
							if (section == 'audio') {
								audio {
									codec((s.'CodecID/Hint' ?: s.'Format').replaceAll(/\p{Punct}/, '').trim())
									language(s.'Language/String3')
									channels(s.'Channel(s)')
								}
							}
							if (section == 'text') {
								subtitle {
									language(s.'Language/String3')
								}
							}
						}
					}
				}
			}
			imdb(id:"tt" + (i.imdbId ?: 0).pad(7), "http://www.imdb.com/title/tt" + (i.imdbId ?: 0).pad(7))
			tmdb(id:i.id, "http://www.themoviedb.org/movie/${i.id}")
		}
	}
	.saveAs(outputFile)
}

def fetchMovieArtworkAndNfo(movieDir, movie, movieFile = null, fetchAll = false, override = false, locale = _args.locale) {
	_guarded {
		def movieInfo = TheMovieDB.getMovieInfo(movie, locale)
		
		// fetch nfo
		fetchMovieNfo(movieDir['movie.nfo'], movieInfo, movieFile, override)
		
		// generate url files
		[[db:'imdb', id:movieInfo.imdbId, url:"http://www.imdb.com/title/tt" + (movieInfo.imdbId ?: 0).pad(7)], [db:'tmdb', id:movieInfo.id, url:"http://www.themoviedb.org/movie/${movieInfo.id}"]].each{
			if (it.id > 0) {
				def content = "[InternetShortcut]\nURL=${it.url}\n"
				content.saveAs(new File(movieDir, "${it.db}.url"))
			}
		}
		
		// fetch series banner, fanart, posters, etc
		fetchMovieArtwork(movieDir['poster.jpg'], movieInfo, 'posters', override, locale)
		fetchMovieArtwork(movieDir['fanart.jpg'], movieInfo, 'backdrops', override, locale)
		
		fetchMovieFanart(movieDir['clearart.png'], movieInfo, 'movieart', null, override, locale)
		fetchMovieFanart(movieDir['logo.png'], movieInfo, 'movielogo', null, override, locale)
		['bluray', 'dvd', null].findResult { diskType -> fetchMovieFanart(movieDir['disc.png'], movieInfo, 'moviedisc', diskType, override, locale) }
		
		if (fetchAll) {
			fetchAllMovieArtwork(movieDir['backdrops'], movieInfo, 'backdrops', override, locale)
		}
	}
}
