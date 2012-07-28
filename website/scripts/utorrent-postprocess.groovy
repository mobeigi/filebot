// filebot -script "fn:utorrent-postprocess" --output "X:/media" --action copy --conflict override --def xbmc=localhost "ut_dir=%D" "ut_file=%F" "ut_kind=%K" "ut_label=%L" "ut_state=%S"
def input = []
def failOnError = _args.conflict == 'fail'

// print input parameters
_args.bindings?.each{ println "Parameter: $it.key = $it.value" }

if (args.empty) {
	// assume we're called with utorrent parameters
	if (ut_kind == "single") {
		input += new File(ut_dir, ut_file) // single-file torrent
	} else {
		input += new File(ut_dir).getFiles() // multi-file torrent
	}
} else {
	// assume we're called normally with arguments
	input += args.getFiles()
}

// extract archives if necessary
input += extract(file:input, output:".", conflict:"override")

// process only media files
input = input.findAll{ it.isVideo() || it.isSubtitle() }

// ignore clutter files
input = input.findAll{ !(it.path =~ /\b(?i:sample|trailer|extras|deleted.scenes|music.video|scrapbook)\b/) }

// print input fileset
input.each{ println "Input: $it" }

// artwork/nfo utility
include("fn:lib/htpc")

// group episodes/movies and rename according to XBMC standards
def groups = input.groupBy{ f ->
	def tvs = detectSeriesName(f)
	def mov = (parseEpisodeNumber(f) || parseDate(f)) ? null : detectMovie(f, false) // skip movie detection if we can already tell it's an episode
	println "$f.name [series: $tvs, movie: $mov]"
	
	// DECIDE EPISODE VS MOVIE (IF NOT CLEAR)
	if (tvs && mov) {
		def norm = { s -> s.lower().space(' ') }
		def dn = norm(guessMovieFolder(f)?.name ?: '')
		def fn = norm(f.nameWithoutExtension)
		def sn = norm(tvs)
		def mn = norm(mov.name)
		
		// S00E00 | 2012.07.21 | One Piece 217 | Firefly - Serenity | [Taken 1, Taken 2, Taken 3, Taken 4, ..., Taken 10]
		if (parseEpisodeNumber(fn, true) || parseDate(fn) || (fn =~ sn && parseEpisodeNumber(fn.after(sn), false)) || fn.after(sn) =~ / - .+/ || f.dir.listFiles{ it.isVideo() && norm(it.name) =~ sn && it.name =~ /\b\d{1,3}\b/}.size() >= 10) {
			println "Exclude Movie: $mov"
			mov = null
		} else if ((detectMovie(f, true) && [dn, fn].find{ it =~ /(19|20)\d{2}/ }) || [dn, fn].find{ it =~ mn && !(it.after(mn) =~ /\b\d{1,3}\b/) }) {
			println "Exclude Series: $tvs"
			tvs = null
		}
	}
	
	// CHECK CONFLICT
	if (((mov && tvs) || (!mov && !tvs)) && failOnError) {
		throw new Exception("Media detection failed")
	}
	
	return [tvs:tvs, mov:mov]
}

groups.each{ group, files ->
	// fetch subtitles
	files += getMissingSubtitles(file:files, output:"srt", encoding:"utf-8")
	
	// EPISODE MODE
	if (group.tvs && !group.mov) {
		def dest = rename(file:files, format:'TV Shows/{n}/{episode.special ? "Special" : "Season "+s}/{n} - {episode.special ? "S00E"+special.pad(2) : s00e00} - {t}', db:'TheTVDB')
		if (dest || failOnError) {
			dest.mapByFolder().each{ dir, fs ->
				println "Fetching artwork for $dir from TheTVDB"
				def sxe = fs.findResult{ eps -> parseEpisodeNumber(eps) }
				def options = TheTVDB.search(group.tvs)
				if (options.isEmpty()) {
					println "TV Series not found: $group.tvs"
					return
				}
				options = options.sortBySimilarity(group.tvs, { opt -> opt.name })
				fetchSeriesArtworkAndNfo(dir.dir, dir, options[0], sxe && sxe.season > 0 ? sxe.season : 1)
			}
		}
	}
	
	// MOVIE MODE
	if (group.mov && !group.tvs) {
		def dest = rename(file:files, format:'Movies/{n} ({y})/{n} ({y}){" CD$pi"}{".$lang"}', db:'TheMovieDB')
		if (dest || failOnError) {
			dest.mapByFolder().each{ dir, fs ->
				println "Fetching artwork for $dir from TheMovieDB"
				fetchMovieArtworkAndNfo(dir, group.mov)
			}
		}
	}
}



// make xbmc or plex scan for new content
xbmc.split(/[\s,|]+/).each{
	println "Notify XBMC: $it"
	invokeScanVideoLibrary(it)
	
	println "Notify Plex: $it"
	refreshPlexLibrary(it)
}
