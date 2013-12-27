import  org.tukaani.xz.*


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
}


/* ------------------------------------------------------------------------- */


// BUILD moviedb index
def isValidMovieName(s) {
	return s=~ /^[A-Z0-9]/ && s =~ /[\p{Alpha}]{3}/
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
	if (tmdb_index.containsKey(m[0])) {
		return tmdb_index[m[0]]
	}
	
	def sync = System.currentTimeMillis()
	def row = [sync, m[0].pad(7), 0, m[2], m[1]]
	try {
		def info = net.sourceforge.filebot.WebServices.TMDb.getMovieInfo("tt${m[0]}", Locale.ENGLISH, false)
		def names = [info.name, info.originalName, m[1]]
		row = [sync, m[0].pad(7), info.id.pad(7), info.released?.year ?: m[2]] + names.findResults{ it ?: '' }
	} catch(FileNotFoundException e) {
	}
	
	println row
	tmdb_txt << row.join('\t') << '\n'
	return row
}
tmdb*.join('\t').join('\n').saveAs(tmdb_txt)

movies = tmdb.findResults{
	def ity = it[1..3] // imdb id, tmdb id, year
	def names = it[4..-2].findAll{ isValidMovieName(it) }.unique{ it.toLowerCase().normalizePunctuation() }
	if (ity[0].toInteger() > 0 && ity[1].toInteger() > 0 && names.size() > 0)
		return ity + names
	else
		return null
}
movies = treeSort(movies, { it[3, 2].join(' ') })

pack(moviedb_out, movies*.join('\t'))
println "Movie Count: " + movies.size()

// sanity check
if (movies.size() < 50000) { throw new Exception('Movie index sanity failed') }


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
tvdb.values().each{
	def n1 = it[2].trim()
	def n2 = it[3].replaceAll(/^(?i)(The|A)\s/, '').replaceAll(/\s&\s/, ' and ').replaceAll(/\([^\)]*\)$/, '').trim()
	
	thetvdb_index << [it[0], n1]
	if (similarity(n1,n2) < 1) {
		thetvdb_index << [it[0], n2]
	}
}

def addSeriesAlias = { from, to ->
	def se = thetvdb_index.find{ from == it[1] }
	if (se == null) throw new Exception("Unabled to find series '${from}'")
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


thetvdb_index = thetvdb_index.findResults{ [it[0] as Integer, it[1].replaceAll(/\s+/, ' ').trim()] }.findAll{ !(it[1] =~ /(?i:duplicate)/ || it[1] =~ /\d{6,}/ || it[1].startsWith('*') || it[1].endsWith('*') || it[1].length() < 2) }
thetvdb_index = thetvdb_index.sort({ a, b -> a[0] <=> b[0] } as Comparator)

// join and sort
def thetvdb_txt = thetvdb_index.groupBy{ it[0] }.findResults{ k, v -> ([k.pad(6)] + v*.getAt(1).unique{ it.toLowerCase() }).join('\t') }

pack(thetvdb_out, thetvdb_txt)
println "TheTVDB Index: " + thetvdb_txt.size()

// sanity check
if (thetvdb_txt.size() < 30000) { throw new Exception('TheTVDB index sanity failed') }


/* ------------------------------------------------------------------------- */


// BUILD anidb index
def anidb = new net.sourceforge.filebot.web.AnidbClient('filebot', 4).getAnimeTitles()

def anidb_index = anidb.findResults{
	def row = []
	row += it.getAnimeId().pad(5)
	row += it.effectiveNames*.replaceAll(/\s+/, ' ')*.replaceAll(/['`´‘’ʻ]+/, /'/)*.trim().unique()
	return row
}

// join and sort
def anidb_txt = anidb_index.findResults{ row -> row.join('\t') }.sort().unique()
pack(anidb_out, anidb_txt)
println "AniDB Index: " + anidb_txt.size()

// sanity check
if (anidb_txt.size() < 8000) { throw new Exception('AniDB index sanity failed') }
