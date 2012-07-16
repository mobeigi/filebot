// filebot -script BuildData.groovy


def sortRegexList(path) {
	def set = new TreeSet(String.CASE_INSENSITIVE_ORDER)
	new File(path).eachLine('UTF-8'){
		// check if regex compiles
		set += java.util.regex.Pattern.compile(it).pattern()
	}
	
	def out = set.join('\n').saveAs(path)
	println "$out\n$out.text\n"
}


// sort and check shared regex collections
sortRegexList("website/data/release-groups.txt")
sortRegexList("website/data/query-blacklist.txt")


// ------------------------------------------------------------------------- //


def series_out = new File("website/data/series.list.gz")
def movies_out = new File("website/data/movies.txt.gz")

def gz(file, lines) {
	file.withOutputStream{ out ->
		new java.util.zip.GZIPOutputStream(out).withWriter('UTF-8'){ writer ->
			lines.each{ writer.append(it).append('\n') }
		}
	}
}


// ------------------------------------------------------------------------- //


// LOAD osdb-imdb.txt (already verified data)
def imdb_tsv = new File("website/data/osdb-imdb.txt")
def imdb = [].asSynchronized() // thread-safe list

imdb_tsv.getText('UTF-8').eachLine{
	imdb << it.split(/\t/)
}
imdb_ids = new HashSet(imdb.collect{ it[0] })

// BUILD movies.txt.gz
def osdb_tsv = new URL("http://www.opensubtitles.org/addons/export_movie.php")
def osdb = []
osdb_tsv.getText('UTF-8').eachLine{
	def line = it.split(/\t/)*.replaceAll(/\s+/, ' ')*.trim()
	if (line.size() == 4 && line[0] =~ /\d+/) {
		osdb << [line[1].toInteger(), line[2], line[3].toInteger()]
	}
}
osdb = osdb.findAll{ it[0] <= 9999999 && it[2] >= 1930 && it[1] =~ /^[A-Z0-9]/ && it[1] =~ /[\p{Alpha}]{3}/ }.collect{ [it[0].pad(7), it[1], it[2]] }


parallel(osdb.collect{ row ->
	return {		
		// update new data
		if (!imdb_ids.contains(row[0])) {
			def mov = net.sourceforge.filebot.WebServices.IMDb.getMovieDescriptor(row[0] as int, null)
			if (mov != null && mov.name.length() > 0 && mov.year > 0) {
				println "Adding $mov"
				imdb << [row[0], mov.name, mov.year]
			} else {
				println "Blacklisting $row"
				imdb << [row[0], null]
			}
		}
	}
}, 20)

// save updated imdb data
imdb.collect{ it.join('\t') }.join('\n').saveAs(imdb_tsv)

// save movie data
def movies = imdb.findAll{ it.size() >= 3 && !it[1].startsWith('"') }

def movieSorter = new TreeMap(String.CASE_INSENSITIVE_ORDER)
movies.each{ movieSorter.put(it[1]+it[2], it) }
movies = movieSorter.values().collect{ it.join('\t') }

gz(movies_out, movies)
println "Movie Count: " + movies.size()


// ------------------------------------------------------------------------- //


// BUILD series.list.gz

// TheTVDB
def thetvdb_index = new URL('http://thetvdb.com/?string=&searchseriesid=&tab=listseries&function=Search')
def thetvdb_names = thetvdb_index.fetch().getHtml('UTF-8')
.depthFirst().TABLE.find{it['@id'] == "listtable"}
.depthFirst().TR.findAll{ it.TD.size() == 3 && it.TD[1].text() == 'English'}
.findResults{ it.TD[0].A.text() }

// AniDB
def anidb_names = net.sourceforge.filebot.WebServices.AniDB.getAnimeTitles().findResults{ [it.getPrimaryTitle(), it.getOfficialTitle('en')] }.flatten()

/*
// IMDb series list
def imdb_series_names = imdb.findAll{ it.size() >= 3 && it[1].startsWith('"') }.collect{ it[1] }

// Dokuwiki list
def dokuwiki_index = new URL('http://docuwiki.net/postbot/getList.php?subject=Name')
def doku_names = []
dokuwiki_index.getText('UTF-8').eachLine{
	doku_names << it.trim().replaceTrailingBrackets()
}
*/

def names = [thetvdb_names, anidb_names]
names.each{ if (it.size() == 0) throw new Exception("Failed to scrape series names") } // sanity check
names = names.flatten().findAll{ it =~ /^[A-Z0-9]/ && it =~ /[\p{Alpha}]{3}/}.findResults{ net.sourceforge.filebot.similarity.Normalization.normalizePunctuation(it) } // collect and normalize names

def seriesSorter = new TreeSet(String.CASE_INSENSITIVE_ORDER)
seriesSorter.addAll(names)
names = seriesSorter as List


gz(series_out, names)
println "Series Count: " + names.size()
