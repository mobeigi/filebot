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
if (args.size() > 0 && (tryQuietly{ ut_dir }?.size() > 0 || tryQuietly{ ut_file }?.size() > 0)) {
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

// myepisodes updates and email notifications
def myepisodes = tryQuietly { myepisodes.split(':', 2) }
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
	tryQuietly{ ut_label } =~ /^(?i:Movie|Couch.Potato)/ || f.path =~ /(?<=tt)\\d{7}/ || tryQuietly{ f.metadata?.object?.class.name =~ /Movie/ }
}

def forceSeries(f) {
	tryQuietly{ ut_label } =~ /^(?i:TV|Kids.Shows)/ || parseEpisodeNumber(f.path) || parseDate(f.path) || f.path =~ /(?i:Season)\D?[0-9]{1,2}/ || tryQuietly{ f.metadata?.object?.class.name =~ /Episode/ }
}

def forceAnime(f) {
	tryQuietly{ ut_label } =~ /^(?i:Anime)/ || (f.isVideo() && (f.name =~ "[\\(\\[]\\p{XDigit}{8}[\\]\\)]" || getMediaInfo(file:f, format:'''{media.AudioLanguageList} {media.TextCodecList}''').tokenize().containsAll(['Japanese', 'ASS'])))
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
def tempFiles = []
input = input.flatten{ f ->
	if (f.isArchive() || f.hasExtension('001')) {
		def extractDir = new File(f.dir, f.nameWithoutExtension)
		def extractFiles = extract(file: f, output: new File(extractDir, f.dir.name), conflict: 'override', filter: { it.isArchive() || it.isVideo() || it.isSubtitle() || (music && it.isAudio()) }, forceExtractAll: true) ?: []
		tempFiles += extractDir
		tempFiles += extractFiles
		return extractFiles
	}
	return f
}

// sanitize input
input = input.findAll{ it?.exists() }.collect{ it.canonicalFile }.unique()

// process only media files
input = input.findAll{ it.isVideo() || it.isSubtitle() || it.isDisk() || (music && it.isAudio()) }

// ignore clutter files
input = input.findAll{ !(it.path =~ /\b(?i:sample|trailer|extras|deleted.scenes|music.video|scrapbook|behind.the.scenes)\b/ || (it.isFile() && it.length() < minFileSize)) }

// print input fileset
input.each{ f -> _log.finest("Input: $f") }

// artwork/nfo utility
include('fn:lib/htpc')

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
		
		// S00E00 | 2012.07.21 | One Piece 217 | Firefly - Serenity | [Taken 1, Taken 2, Taken 3, Taken 4, ..., Taken 10]
		if ((parseEpisodeNumber(fn, true) || parseDate(fn) || ([dn, fn].find{ it =~ sn && matchMovie(it, true) == null } && (parseEpisodeNumber(fn.after(sn), false) || fn.after(sn) =~ /\d{1,2}\D+\d{1,2}/) && matchMovie(fn, true) == null) || (fn.after(sn) ==~ /.{0,3} - .+/ && matchMovie(fn, true) == null) || f.dir.listFiles{ it.isVideo() && norm(it.name) =~ sn && it.name =~ /\b\d{1,3}\b/}.size() >= 10) && !tryQuietly{ def m = detectMovie(f, true); m.year >= 1950 && f.listPath().reverse().take(3).find{ it.name =~ m.year } }) {
			_log.fine("Exclude Movie: $mov")
			mov = null
		} else if (mn ==~ fn || (detectMovie(f, true) && [dn, fn].find{ it =~ /(19|20)\d{2}/ }) || [dn, fn].find{ it =~ mn && !(it.after(mn) =~ /\b\d{1,3}\b/) && !(it.before(mn).contains(sn)) }) {
			_log.fine("Exclude Series: $tvs")
			tvs = null
		}
	}
	
	// CHECK CONFLICT
	if (((mov && tvs) || (!mov && !tvs)) && failOnError) {
		throw new Exception("Media detection failed")
	}
	
	return [tvs: tvs, mov: mov, anime: null]
}

// log movie/series/anime detection results
groups.each{ group, files -> _log.finest("Group: $group => ${files*.name}") }

// process each batch
groups.each{ group, files ->
	// fetch subtitles (but not for anime)
	if (subtitles && !group.anime) {
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
				def options = TheTVDB.search(config.name)
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
	if (group.mov && !group.tvs && !group.anime) {
		def dest = rename(file:files, format:format.mov, db:'TheMovieDB')
		if (dest && artwork) {
			dest.mapByFolder().each{ dir, fs ->
				_log.finest "Fetching artwork for $dir from TheMovieDB"
				fetchMovieArtworkAndNfo(dir, group.mov, fs.findAll{ it.isVideo() }.sort{ it.length() }.reverse().findResult{ it }, backdrops)
			}
		}
		if (dest == null && failOnError) {
			throw new Exception("Failed to rename movie: $group.mov")
		}
	}
	
	// MUSIC MODE
	if (group.music) {
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
	include('fn:update-mes', [login:myepisodes.join(':'), addshows:true], getRenameLog().values())
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
		_log.info 'Clean clutter files and empty folders'
		include('fn:cleaner', [root:true], !args.empty ? args : ut_kind == 'multi' && ut_dir ? [ut_dir as File] : [])
	}
}
