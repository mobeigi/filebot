// filebot -script "fn:amc" --output "X:/media" --action copy --conflict override --def subtitles=en music=y artwork=y "ut_dir=%D" "ut_file=%F" "ut_kind=%K" "ut_title=%N" "ut_label=%L" "ut_state=%S"
def input = []
def failOnError = _args.conflict == 'fail'

// print input parameters
_args.bindings?.each{ _log.fine("Parameter: $it.key = $it.value") }
args.each{ _log.fine("Argument: $it") }
args.findAll{ !it.exists() }.each{ throw new Exception("File not found: $it") }

// check user-defined pre-condition
if (tryQuietly{ !(ut_state ==~ ut_state_allow) }) {
	throw new Exception("Invalid state: ut_state = $ut_state (expected $ut_state_allow)")
}

// check ut mode vs standalone mode
if ((args.size() > 0 && (tryQuietly{ ut_dir }?.size() > 0 || tryQuietly{ ut_file }?.size() > 0)) || (args.size() == 0 && (tryQuietly{ ut_dir } == null && tryQuietly{ ut_file } == null))) {
	throw new Exception("Conflicting arguments: pass in either file arguments or ut_dir/ut_file parameters but not both")
}

// enable/disable features as specified via --def parameters
def music     = tryQuietly{ music.toBoolean() }
def subtitles = tryQuietly{ subtitles.toBoolean() ? ['en'] : subtitles.split(/[ ,|]+/).findAll{ it.length() >= 2 } }
def artwork   = tryQuietly{ artwork.toBoolean() }
def backdrops = tryQuietly{ backdrops.toBoolean() }
def clean     = tryQuietly{ clean.toBoolean() }
def exec      = tryQuietly{ exec.toString() }

// array of xbmc/plex hosts
def xbmc = tryQuietly{ xbmc.split(/[ ,|]+/) }
def plex = tryQuietly{ plex.split(/[ ,|]+/) }

// extra options, myepisodes updates and email notifications
def deleteAfterExtract = tryQuietly{ deleteAfterExtract.toBoolean() }
def excludeList = tryQuietly{ new File(_args.output, excludeList) }
def myepisodes = tryQuietly{ myepisodes.split(':', 2) }
def gmail = tryQuietly{ gmail.split(':', 2) }
def pushover = tryQuietly{ pushover.toString() }

// user-defined filters
def minFileSize = tryQuietly{ minFileSize.toLong() }; if (minFileSize == null) { minFileSize = 0 };

// series/anime/movie format expressions
def format = [
	tvs:   tryQuietly{ seriesFormat } ?: '''TV Shows/{n}/{episode.special ? "Special" : "Season "+s.pad(2)}/{n} - {episode.special ? "S00E"+special.pad(2) : s00e00} - {t.replaceAll(/[`´‘’ʻ]/, "'").replaceAll(/[!?.]+$/).replacePart(', Part $1')}{".$lang"}''',
	anime: tryQuietly{ animeFormat  } ?: '''Anime/{n}/{n} - {sxe} - {t.replaceAll(/[!?.]+$/).replaceAll(/[`´‘’ʻ]/, "'").replacePart(', Part $1')}''',
	mov:   tryQuietly{ movieFormat  } ?: '''Movies/{n} ({y})/{n} ({y}){" CD$pi"}{".$lang"}''',
	music: tryQuietly{ musicFormat  } ?: '''Music/{n}/{album+'/'}{pi.pad(2)+'. '}{artist} - {t}'''
]


// force movie/series/anime logic
def forceMovie(f) {
	tryQuietly{ ut_label } =~ /^(?i:Movie|Couch.Potato)/ || f.dir.path =~ /\b(?i:Movies)\b/ || f.path =~ /(?<=tt)\\d{7}/ || tryQuietly{ f.metadata?.object?.class.name =~ /Movie/ }
}

def forceSeries(f) {
	tryQuietly{ ut_label } =~ /^(?i:TV|Kids.Shows)/ || f.dir.path =~ /\b(?i:TV.Shows)\b/ || parseEpisodeNumber(f.path) || parseDate(f.path) || f.path =~ /(?i:Season)\D?[0-9]{1,2}\D/ || tryQuietly{ f.metadata?.object?.class.name =~ /Episode/ }
}

def forceAnime(f) {
	tryQuietly{ ut_label } =~ /^(?i:Anime)/ || f.dir.path =~ /\b(?i:Anime)\b/ || (f.isVideo() && (f.name =~ "[\\(\\[]\\p{XDigit}{8}[\\]\\)]" || getMediaInfo(file:f, format:'''{media.AudioLanguageList} {media.TextCodecList}''').tokenize().containsAll(['Japanese', 'ASS'])))
}

def forceIgnore(f) {
	tryQuietly{ ut_label } =~ /^(?i:ebook|other|ignore)/ || f.path =~ tryQuietly{ ignore }
}


// specify how to resolve input folders, e.g. grab files from all folders except disk folders
def resolveInput(f) {
	if (f.isDirectory() && !f.isDisk())
		return f.listFiles().toList().findResults{ resolveInput(it) }
	else
		return f
}

// collect input fileset as specified by the given --def parameters
if (args.empty) {
	// assume we're called with utorrent parameters (account for older and newer versions of uTorrents)
	if (ut_kind == 'single' || (ut_kind != 'multi' && ut_dir && ut_file)) {
		input += new File(ut_dir, ut_file) // single-file torrent
	} else {
		input += resolveInput(ut_dir as File) // multi-file torrent
	}
} else {
	// assume we're called normally with arguments
	input += args.findResults{ resolveInput(it) }
}


// flatten nested file structure
input = input.flatten()

// extract archives (zip, rar, etc) that contain at least one video file
def extractedArchives = []
def tempFiles = []
input = input.flatten{ f ->
	if (f.isArchive() || f.hasExtension('001')) {
		def extractDir = new File(f.dir, f.nameWithoutExtension)
		def extractFiles = extract(file: f, output: new File(extractDir, f.dir.name), conflict: 'skip', filter: { it.isArchive() || it.isVideo() || it.isSubtitle() || (music && it.isAudio()) }, forceExtractAll: true) ?: []
		
		if (extractFiles.size() > 0) {
			extractedArchives += f
			tempFiles += extractDir
			tempFiles += extractFiles
		}
		return extractFiles
	}
	return f
}

// sanitize input
input = input.findAll{ it?.exists() }.collect{ it.canonicalFile }.unique()

// process only media files
input = input.findAll{ f -> (f.isVideo() && !tryQuietly{ f.hasExtension('iso') && !f.isDisk() }) || f.isSubtitle() || (f.isDirectory() && f.isDisk()) || (music && f.isAudio()) }

// ignore clutter files
input = input.findAll{ f -> !(f.path =~ /\b(?i:sample|trailer|extras|music.video|scrapbook|behind.the.scenes|extended.scenes|deleted.scenes|s\d{2}c\d{2}|mini.series)\b/ || (f.isFile() && f.length() < minFileSize)) }

// check and update exclude list (e.g. to make sure files are only processed once)
if (excludeList) {
	// check excludes from previous runs
	def excludePathSet = excludeList.exists() ? excludeList.text.split('\n') as HashSet : []
	input = input.findAll{ f -> !excludePathSet.contains(f.path) }
	
	// update excludes with input of this run
	excludePathSet += input
	excludePathSet.join('\n').saveAs(excludeList)
}

// print input fileset
input.each{ f -> _log.finest("Input: $f") }

// artwork/nfo utility
if (artwork || xbmc || plex) { include('fn:lib/htpc') }

// group episodes/movies and rename according to XBMC standards
def groups = input.groupBy{ f ->
	// skip auto-detection if possible
	if (forceIgnore(f))
		return []
	if (f.isAudio() && !f.isVideo()) // PROCESS MUSIC FOLDER BY FOLDER
		return [music: f.dir.name]
	if (forceMovie(f))
		return [mov:   detectMovie(f, false)]
	if (forceSeries(f))
		return [tvs:   detectSeriesName(f) ?: detectSeriesName(f.dir.listFiles{ it.isVideo() })]
	if (forceAnime(f))
		return [anime: detectSeriesName(f) ?: detectSeriesName(f.dir.listFiles{ it.isVideo() })]
	
	
	def tvs = detectSeriesName(f)
	def mov = detectMovie(f, false)
	_log.fine("$f.name [series: $tvs, movie: $mov]")
	
	// DECIDE EPISODE VS MOVIE (IF NOT CLEAR)
	if (tvs && mov) {
		def norm = { s -> s.ascii().normalizePunctuation().lower().space(' ') }
		def dn = norm(guessMovieFolder(f)?.name ?: '')
		def fn = norm(f.nameWithoutExtension)
		def sn = norm(tvs)
		def mn = norm(mov.name)
		
		/**
		println '--- EPISODE FILTER (POS) ---'
		println parseEpisodeNumber(fn, true) || parseDate(fn)
		println ([dn, fn].find{ it =~ sn && matchMovie(it, true) == null } && (parseEpisodeNumber(stripReleaseInfo(fn.after(sn), false), false) || fn.after(sn) =~ /\D\d{1,2}\D{1,3}\d{1,2}\D/) && matchMovie(fn, true) == null)
		println (fn.after(sn) ==~ /.{0,3} - .+/ && matchMovie(fn, true) == null)
		println f.dir.listFiles{ it.isVideo() && (dn =~ sn || norm(it.name) =~ sn) && it.name =~ /\d{1,3}/}.findResults{ it.name.matchAll(/\d{1,3}/) as Set }.unique().size() >= 10
		println '--- EPISODE FILTER (NEG) ---'
		println (mov.year >= 1950 && f.listPath().reverse().take(3).find{ it.name =~ mov.year })
		println (mn =~ sn && [dn, fn].find{ it =~ /(19|20)\d{2}/ })
		println '--- MOVIE FILTER (POS) ---'
		println (similarity(mn, fn) >= 0.8 || [dn, fn].find{ it.findAll( ~/\d{4}/ ).findAll{ y -> [mov.year-1, mov.year, mov.year+1].contains(y.toInteger()) }.size() > 0 } != null)
		println ([dn, fn].find{ it =~ mn && !(it.after(mn) =~ /\b\d{1,3}\b/) && (similarity(it, mn) > 0.2 + similarity(it, sn)) } != null)
		println (detectMovie(f, true) && [dn, fn].find{ it =~ /(19|20)\d{2}/ } != null)
		**/
		
		// S00E00 | 2012.07.21 | One Piece 217 | Firefly - Serenity | [Taken 1, Taken 2, Taken 3, Taken 4, ..., Taken 10]
		if ((parseEpisodeNumber(fn, true) || parseDate(fn) || ([dn, fn].find{ it =~ sn && matchMovie(it, true) == null } && (parseEpisodeNumber(stripReleaseInfo(fn.after(sn), false), false) || fn.after(sn) =~ /\D\d{1,2}\D{1,3}\d{1,2}\D/) && matchMovie(fn, true) == null) || (fn.after(sn) ==~ /.{0,3} - .+/ && matchMovie(fn, true) == null) || f.dir.listFiles{ it.isVideo() && (dn =~ sn || norm(it.name) =~ sn) && it.name =~ /\d{1,3}/}.findResults{ it.name.matchAll(/\d{1,3}/) as Set }.unique().size() >= 10 || mov.year < 1900) && !( (mov.year >= 1950 && f.listPath().reverse().take(3).find{ it.name =~ mov.year }) || (mn =~ sn && [dn, fn].find{ it =~ /(19|20)\d{2}/ }) ) ) {
			_log.fine("Exclude Movie: $mov")
			mov = null
		} else if ((similarity(mn, fn) >= 0.8 || [dn, fn].find{ it.findAll( ~/\d{4}/ ).findAll{ y -> [mov.year-1, mov.year, mov.year+1].contains(y.toInteger()) }.size() > 0 } != null) || ([dn, fn].find{ it =~ mn && !(it.after(mn) =~ /\b\d{1,3}\b/) && (similarity(it, mn) > 0.2 + similarity(it, sn)) } != null) || (detectMovie(f, true) && [dn, fn].find{ it =~ /(19|20)\d{2}/ } != null)) {
			_log.fine("Exclude Series: $tvs")
			tvs = null
		}
	}
	
	// CHECK CONFLICT
	if (((mov && tvs) || (!mov && !tvs))) {
		if (failOnError) {
			throw new Exception("Media detection failed")
		} else {
			_log.fine("Unable to differentiate: [$f.name] => [$tvs] VS [$mov]")
			return [tvs: null, mov: null, anime: null]
		}
	}
	
	return [tvs: tvs, mov: mov, anime: null]
}

// log movie/series/anime detection results
groups.each{ group, files -> _log.finest("Group: $group => ${files*.name}") }

// process each batch
groups.each{ group, files ->
	// fetch subtitles (but not for anime)
	if (subtitles && !group.anime && files.findAll{ it.isVideo() }.size() > 0) {
		subtitles.each{ languageCode ->
			def subtitleFiles = getMissingSubtitles(file:files, output:'srt', encoding:'UTF-8', lang:languageCode, strict:true) ?: []
			files += subtitleFiles
			tempFiles += subtitleFiles // if downloaded for temporarily extraced files delete later
		}
	}
	
	// EPISODE MODE
	if ((group.tvs || group.anime) && !group.mov) {
		// choose series / anime config
		def config = group.tvs ? [name:group.tvs,   format:format.tvs,   db:'TheTVDB', seasonFolder:true ]
		                       : [name:group.anime, format:format.anime, db:'AniDB',   seasonFolder:false]
		def dest = rename(file: files, format: config.format, db: config.db)
		if (dest && artwork) {
			dest.mapByFolder().each{ dir, fs ->
				_log.finest "Fetching artwork for $dir from TheTVDB"
				def sxe = fs.findResult{ eps -> parseEpisodeNumber(eps) }
				def options = TheTVDB.search(detectSeriesName(fs), _args.locale)
				if (options.isEmpty()) {
					_log.warning "TV Series not found: $config.name"
					return
				}
				options = options.sortBySimilarity(config.name, { s -> s.name })
				fetchSeriesArtworkAndNfo(config.seasonFolder ? dir.dir : dir, dir, options[0], sxe && sxe.season > 0 ? sxe.season : 1)
			}
		}
		if (dest == null && failOnError) {
			throw new Exception("Failed to rename series: $config.name")
		}
	}
	
	// MOVIE MODE
	else if (group.mov && !group.tvs && !group.anime) {
		def dest = rename(file:files, format:format.mov, db:'TheMovieDB')
		if (dest && artwork) {
			dest.mapByFolder().each{ dir, fs ->
				_log.finest "Fetching artwork for $dir from TheMovieDB"
				def movieFile = fs.findAll{ it.isVideo() }.sort{ it.length() }.reverse().findResult{ it }
				fetchMovieArtworkAndNfo(dir, detectMovie(movieFile), movieFile, backdrops)
			}
		}
		if (dest == null && failOnError) {
			throw new Exception("Failed to rename movie: $group.mov")
		}
	}
	
	// MUSIC MODE
	else if (group.music) {
		def dest = rename(file:files, format:format.music, db:'AcoustID')
		if (dest == null && failOnError) {
			throw new Exception("Failed to rename music: $group.music")
		}
	}
}

// skip notifications if nothing was renamed anyway
if (getRenameLog().isEmpty()) {
	return
}

// run program on newly processed files
if (exec) {
	getRenameLog().each{ from, to ->
		def command = getMediaInfo(format: exec, file: to)
		_log.finest("Execute: $command")
		execute(command)
	}
}

// make XMBC scan for new content and display notification message
if (xbmc) {
	xbmc.each{ host ->
		_log.info "Notify XBMC: $host"
		_guarded{
			showNotification(host, 9090, 'FileBot', "Finished processing ${tryQuietly { ut_title } ?: input*.dir.name.unique()} (${getRenameLog().size()} files).", 'http://www.filebot.net/images/icon.png')
			scanVideoLibrary(host, 9090)
		}
	}
}

// make Plex scan for new content
if (plex) {
	plex.each{
		_log.info "Notify Plex: $it"
		refreshPlexLibrary(it)
	}
}

// mark episodes as 'acquired'
if (myepisodes) {
	_log.info 'Update MyEpisodes'
	executeScript('fn:update-mes', [login:myepisodes.join(':'), addshows:true], getRenameLog().values())
}

if (pushover) {
	// include webservice utility
	include('fn:lib/ws')
	
	_log.info 'Sending Pushover notification'
	Pushover(pushover).send("Finished processing ${tryQuietly { ut_title } ?: input*.dir.name.unique()} (${getRenameLog().size()} files).")
}

// send status email
if (gmail) {
	// ant/mail utility
	include('fn:lib/ant')
	
	// send html mail
	def renameLog = getRenameLog()
	def emailTitle = tryQuietly { ut_title } ?: input*.dir.name.unique()
	
	sendGmail(
		subject: "[FileBot] ${emailTitle}",
		message: XML {
			html {
				body {
					p("FileBot finished processing ${emailTitle} (${renameLog.size()} files).");
					hr(); table {
						th("Parameter"); th("Value")
						_args.bindings.findAll{ param -> param.key =~ /^ut_/ }.each{ param ->
							tr { [param.key, param.value].each{ td(it)} }
						}
					}
					hr(); table {
						th("Original Name"); th("New Name"); th("New Location")
						renameLog.each{ from, to ->
							tr { [from.name, to.name, to.parent].each{ cell -> td{ nobr{ code(cell) } } } }
						}
					}
					hr(); small("// Generated by ${net.sourceforge.filebot.Settings.applicationIdentifier} on ${new Date().dateString} at ${new Date().timeString}")
				}
			}
		},
		messagemimetype: 'text/html',
		to: tryQuietly{ mailto } ?: gmail[0] + '@gmail.com', // mail to self by default
		user: gmail[0], password: gmail[1]
	)
}

if (deleteAfterExtract) {
	extractedArchives.each{ a ->
		_log.finest("Delete archive $a")
		a.delete()
		a.dir.listFiles().toList().findAll{ v -> v.name.startsWith(a.nameWithoutExtension) && v.extension ==~ /r\d+/ }.each{ v ->
			_log.finest("Delete archive volume $v")
			v.delete()
		}
	}
}

// clean empty folders, clutter files, etc after move
if (clean) {
	if (['COPY', 'HARDLINK'].find{ it.equalsIgnoreCase(_args.action) } && tempFiles.size() > 0) {
		_log.info 'Clean temporary extracted files'
		// delete extracted files
		tempFiles.findAll{ it.isFile() }.sort().each{
			_log.finest "Delete $it"
			it.delete()
		}
		// delete remaining empty folders
		tempFiles.findAll{ it.isDirectory() }.sort().reverse().each{
			_log.finest "Delete $it"
			if (it.getFiles().isEmpty()) it.deleteDir()
		}
	}
	
	// deleting remaining files only makes sense after moving files
	if ('MOVE'.equalsIgnoreCase(_args.action)) {
		def cleanerInput = !args.empty ? args : ut_kind == 'multi' && ut_dir ? [ut_dir as File] : []
		cleanerInput = cleanerInput.findAll{ f -> f.exists() }
		if (cleanerInput.size() > 0) {
			_log.info 'Clean clutter files and empty folders'
			executeScript('fn:cleaner', args.empty ? [root:true] : [root:false], cleanerInput)
		}
	}
}
