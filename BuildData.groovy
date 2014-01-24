import org.tukaani.xz.*
import net.sourceforge.filebot.media.*

/* ------------------------------------------------------------------------- */


def sortRegexList(path) {
	def set = new TreeSet(String.CASE_INSENSITIVE_ORDER)
	new File(path).eachLine('UTF-8'){
		// check if regex compiles
		set += java.util.regex.Pattern.compile(it.trim()).pattern()
	}
	def out = set.join('\n').saveAs(path)
	println "${out}\n${out.text}\n"
}


// sort and check shared regex collections
sortRegexList("website/data/release-groups.txt")
sortRegexList("website/data/query-blacklist.txt")
sortRegexList("website/data/exclude-blacklist.txt")
sortRegexList("website/data/series-mappings.txt")


/* ------------------------------------------------------------------------- */


def reviews = []
new File('reviews.csv').eachLine('UTF-8'){
	def s = it.split(';', 3)
	reviews << [user: s[0], date: s[1], text: s[2].replaceAll(/^\"|\"$/, '').replaceAll(/["]{2}/, '"') ]
}
reviews = reviews.sort{ it.date }

def json = new groovy.json.JsonBuilder()
json.call(reviews as List)
json.toPrettyString().saveAs('website/reviews.json')
println "Reviews: " + reviews.size()


/* ------------------------------------------------------------------------- */


def moviedb_out = new File("website/data/moviedb.txt")
def thetvdb_out = new File("website/data/thetvdb.txt")
def anidb_out   = new File("website/data/anidb.txt")

def pack(file, lines) {
	new File(file.parentFile, file.name + '.xz').withOutputStream{ out ->
		new XZOutputStream(out, new LZMA2Options(LZMA2Options.PRESET_DEFAULT)).withWriter('UTF-8'){ writer ->
			lines.each{ writer.append(it).append('\n') }
		}
	}
	def rows = lines.size()
	def columns = lines.collect{ it.split(/\t/).length }.max()
	println "$file ($rows rows, $columns columns)"
}


/* ------------------------------------------------------------------------- */


// BUILD moviedb index
def isValidMovieName(s) {
	return s=~ /^[A-Z0-9]/ && s =~ /[\p{Alpha}]{3}/
}

def getNamePermutations(names) {
	def fn1 = { s -> s.replaceAll(/^(?i)(The|A)\s/, '') }
	def fn2 = { s -> s.replaceAll(/\s&\s/, ' and ') }
	def fn3 = { s -> s.replaceAll(/\([^\)]*\)$/, '') }

	def out = new LinkedHashSet(names*.trim()).toList()
	def res = out
	[fn1, fn2, fn3].each{ fn ->
		res = res.findResults{ fn(it) }
	}
	out += res
	
	out = out.findAll{ it.length() >= 2 && !(it =~ /^[a-z]/) && it =~ /^[.\p{L}\p{Digit}]/ } // MUST START WITH UNICODE LETTER
	out = out.findAll{ !MediaDetection.releaseInfo.structureRootPattern.matcher(it).matches() } // IGNORE NAMES THAT OVERLAP WITH MEDIA FOLDER NAMES
	
	out = out.unique{ it.toLowerCase().normalizePunctuation() }.findAll{ it.length() > 0 }
	out = out.size() <= 4 ? out : out.subList(0, 4)
	return out
}

def treeSort(list, keyFunction) {
	def sorter = new TreeMap(String.CASE_INSENSITIVE_ORDER)
	list.each{
		sorter.put(keyFunction(it), it)
	}
	return sorter.values()
}


def omdb = []
new File('omdb.txt').eachLine('Windows-1252'){
	def line = it.split(/\t/)
	if (line.length > 11 && line[0] ==~ /\d+/) {
		def imdbid = line[1].substring(2).toInteger()
		def name = line[2].replaceAll(/\s+/, ' ').trim()
		def year = line[3].toInteger()
		def runtime = line[5]
		def rating = tryQuietly{ line[11].toFloat() } ?: 0
		def votes = tryQuietly{ line[12].replaceAll(/\D/, '').toInteger() } ?: 0
		
		if ((year >= 1970 && (runtime =~ /h/ || votes >= 200) && rating >= 1 && votes >= 50) || (year >= 1950 && votes >= 5000)) {
			omdb << [imdbid.pad(7), name, year]
		}
	}
}
omdb = omdb.findAll{ (it[0] as int) <= 9999999 && isValidMovieName(it[1]) }


def tmdb_txt = new File('tmdb.txt')
def tmdb_index = csv(tmdb_txt, '\t', 1, [0..-1])

def tmdb = omdb.findResults{ m ->
	def sync = System.currentTimeMillis()
	if (tmdb_index.containsKey(m[0]) && (sync - tmdb_index[m[0]][0].toLong()) < (360 * 24 * 60 * 60 * 1000L) ) {
		return tmdb_index[m[0]]
	}
	
	def row = [sync, m[0].pad(7), 0, m[2], m[1]]
	try {
		def info = net.sourceforge.filebot.WebServices.TMDb.getMovieInfo("tt${m[0]}", Locale.ENGLISH, true, false)
		def names = [info.name, info.originalName] + info.alternativeTitles
		if (info.released != null) {
			row = [sync, m[0].pad(7), info.id.pad(7), info.released.year] + names
		} else {
			println "Illegal movie: ${info.name} | ${m}"
		}
	} catch(FileNotFoundException e) {
	}
	
	println row
	tmdb_txt << row.join('\t') << '\n'
	return row
}
tmdb*.join('\t').join('\n').saveAs(tmdb_txt)

movies = tmdb.findResults{
	def ity = it[1..3] // imdb id, tmdb id, year
	def names = getNamePermutations(it[4..-1]).findAll{ isValidMovieName(it) }
	if (ity[0].toInteger() > 0 && ity[1].toInteger() > 0 && names.size() > 0)
		return ity + names
	else
		return null
}
movies = treeSort(movies, { it[3, 2].join(' ') })

// sanity check
if (movies.size() < 50000) { throw new Exception('Movie index sanity failed') }
pack(moviedb_out, movies*.join('\t'))


/* ------------------------------------------------------------------------- */


// BUILD tvdb index
def tvdb_txt = new File('tvdb.txt')
def tvdb = [:]
new File('tvdb.txt').eachLine{
	def line = it.split('\t', 5).toList()
	tvdb.put(line[0] as Integer, [line[0] as Integer, line[1], line[2], line[3], line[4] as Integer])
}

def tvdb_updates = new File('updates_all.xml').text.xml.'**'.Series.findResults{ s -> tryQuietly{ [id:s.id.text() as Integer, time:s.time.text() as Integer] } }
tvdb_updates.each{ update ->
	if (tvdb[update.id] == null || update.time > tvdb[update.id][4]) {
		try {
			retry(2, 500) {
				def xml = new URL("http://thetvdb.com/api/BA864DEE427E384A/series/${update.id}/en.xml").fetch().text.xml
				def imdbid = xml.'**'.IMDB_ID.text()
				def tvdb_name = xml.'**'.SeriesName.text()
				def imdb_name = _guarded{
					if (imdbid =~ /tt(\d+)/) {
						def dom = IMDb.parsePage(IMDb.getMoviePageLink(imdbid.match(/tt(\d+)/) as int).toURL())
						return net.sourceforge.tuned.XPathUtilities.selectString("//META[@property='og:title']/@content", dom)
					}
				}
				def data = [update.id, imdbid ?: '', tvdb_name ?: '', imdb_name ?: '', update.time]
				tvdb.put(update.id, data)
				println "Update $update => $data"
			}
		}
		catch(Throwable e) {
			def data = [update.id, '', '', '', update.time]
			tvdb.put(update.id, data)
			println "Update $update => $data"
		}
	}
}

// remove entries that have become invalid
def tvdb_ids = tvdb_updates.findResults{ it.id } as HashSet
tvdb.keySet().toList().each{ id ->
	if (!tvdb_ids.contains(id)) {
		println "Invalid ID found: ${tvdb[id]}"
		tvdb.remove(id)
	}
}

tvdb.values().findResults{ it.join('\t') }.join('\n').saveAs(tvdb_txt)


def thetvdb_index = []
tvdb.values().each{ r ->
	def tvdb_name = r[2]
	def imdb_name = r[3].replaceAll(/\([^\)]*\)$/, '').trim()
	
	getNamePermutations([tvdb_name, imdb_name]).each{ n ->
		thetvdb_index << [r[0], n]
	}
}

def addSeriesAlias = { from, to ->
	def se = thetvdb_index.find{ from == it[1] && !it.contains(to) }
	if (se == null) throw new Exception("Unabled to find series '${from}': '${to}'")
	thetvdb_index << [se[0], to]
}

// additional custom mappings
addSeriesAlias('Law & Order: Special Victims Unit', 'Law and Order SVU')
addSeriesAlias('Law & Order: Special Victims Unit', 'Law & Order SVU')
addSeriesAlias('CSI: Crime Scene Investigation', 'CSI')
addSeriesAlias('M*A*S*H', 'MASH')
addSeriesAlias('M*A*S*H', 'M.A.S.H.')
addSeriesAlias('NCIS: Los Angeles', 'NCIS LA')
addSeriesAlias('How I Met Your Mother', 'HIMYM')
addSeriesAlias('Battlestar Galactica (2003)', 'BSG')
addSeriesAlias('World Series of Poker', 'WSOP')
addSeriesAlias('English Premier League', 'EPL')
addSeriesAlias('House of Cards', 'HOC')
addSeriesAlias('The Big Bang Theory', 'TBBT')


thetvdb_index = thetvdb_index.findResults{ [it[0] as Integer, it[1].replaceAll(/\s+/, ' ').trim()] }.findAll{ !(it[1] =~ /(?i:duplicate)/ || it[1] =~ /\d{6,}/ || it[1].startsWith('*') || it[1].endsWith('*') || it[1].length() < 2) }
thetvdb_index = thetvdb_index.sort({ a, b -> a[0] <=> b[0] } as Comparator)

// join and sort
def thetvdb_txt = thetvdb_index.groupBy{ it[0] }.findResults{ k, v -> ([k.pad(6)] + v*.getAt(1).unique{ it.toLowerCase() }).join('\t') }

// sanity check
if (thetvdb_txt.size() < 30000) { throw new Exception('TheTVDB index sanity failed') }
pack(thetvdb_out, thetvdb_txt)


/* ------------------------------------------------------------------------- */


// BUILD anidb index
def anidb = new net.sourceforge.filebot.web.AnidbClient('filebot', 4).getAnimeTitles()

def anidb_index = anidb.findResults{
	def names = it.effectiveNames*.replaceAll(/\s+/, ' ')*.trim()*.replaceAll(/['`´‘’ʻ]+/, /'/)
	names = getNamePermutations(names)
	names = names.findAll{ stripReleaseInfo(it)?.length() > 0 }

	return names.empty ? null : [it.getAnimeId().pad(5)] + names
}

// join and sort
def anidb_txt = anidb_index.findResults{ row -> row.join('\t') }.sort().unique()

// sanity check
if (anidb_txt.size() < 8000) { throw new Exception('AniDB index sanity failed') }
pack(anidb_out, anidb_txt)
