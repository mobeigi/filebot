// filebot -script BuildData.groovy


// UPDATE release-groups.txt FROM http://scenegrouplist.com/lists_sgl.php
@Grab(group='org.jsoup', module='jsoup', version='1.7.1')
import org.jsoup.*

/*
def sgl = []
for (def page = 0; true; page++) {
	def dom = Jsoup.parse(new URL('http://scenegrouplist.com/lists_sgl.php?pageNum_RSSGL=' + page), 10000)
	def table = dom.select("table[border=1] tr").collect{ it.select("td")*.text()*.trim() }.findAll{ it[2] =~ /\d{4}/ }
	sgl += table
	if (table.empty) break
}
sgl = sgl.findAll{ it[1] =~ /\b(DVD|DVDR|HD|TV)\b/ && it[0] =~ /\p{Alpha}/}.findResults{ it[0] }
sgl = sgl.collect{ it.before(/ - /).trim().space('.') }.collect{ it ==~ /\p{Upper}?\p{Lower}+/ ? it.toUpperCase() : it }

// append release group names
new File('website/data/release-groups.txt') << '\n' << sgl.join('\n')
*/


// ------------------------------------------------------------------------- //


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
sortRegexList("website/data/exclude-blacklist.txt")
sortRegexList("website/data/series-mappings.txt")


// ------------------------------------------------------------------------- //


def series_out  = new File("website/data/series.list.gz")
def movies_out  = new File("website/data/movies.txt.gz")
def thetvdb_out = new File("website/data/thetvdb.txt.gz")

def gz(file, lines) {
	file.withOutputStream{ out ->
		new java.util.zip.GZIPOutputStream(out).withWriter('UTF-8'){ writer ->
			lines.each{ writer.append(it).append('\n') }
		}
	}
}


// ------------------------------------------------------------------------- //

// BUILD movies.txt.gz
def omdb = new TreeSet({ a, b -> a[0].compareTo(b[0]) } as Comparator)
new File('omdb.txt').eachLine('Windows-1252'){
	def line = it.split(/\t/)
	
	if (line.length > 11 && line[0] ==~ /\d+/) {
		def imdbid = line[1].substring(2).toInteger()
		def name = line[2]
		def year = line[3].toInteger()
		def runtime = line[5]
		def rating = tryQuietly{ line[11].toFloat() } ?: 0
		def votes = tryQuietly{ line[12].replaceAll(/\D/, '').toInteger() } ?: 0
		
		if ((year >= 1970 && runtime =~ /h/ && rating >= 1 && votes >= 50) || (votes >= 2000)) {
			line = line*.replaceAll(/\s+/, ' ')*.trim()
			omdb << [imdbid, name, year]
		}
	}
}
omdb = omdb.findAll{ it[0] <= 9999999 && it[1] =~ /^[A-Z0-9]/ && it[1] =~ /[\p{Alpha}]{3}/ }.collect{ [it[0].pad(7), it[1], it[2]] }

// save movie data
def movies = omdb.findAll{ it.size() >= 3 && !it[1].startsWith('"') }
def movieSorter = new TreeMap(String.CASE_INSENSITIVE_ORDER)
movies.each{ movieSorter.put([it[1], it[2], it[0]].join('\t'), it) }
movies = movieSorter.values().collect{ it.join('\t') }

gz(movies_out, movies)
println "Movie Count: " + movies.size()


// ------------------------------------------------------------------------- //

// BUILD thetvdb-index.gz
def thetvdb_index_url = new URL('http://thetvdb.com/?string=&searchseriesid=&tab=listseries&function=Search')
def thetvdb_index = thetvdb_index_url.fetch().getHtml('UTF-8')
.depthFirst().TABLE.find{it['@id'] == "listtable"}
.depthFirst().TR.findAll{ it.TD.size() == 3 && it.TD[1].text() == 'English' && it.TD[0].A.text() }
.findResults{ [it.TD[2].text(), it.TD[0].A.text()] }

thetvdb_index = thetvdb_index.findResults{ [it[0] as Integer, it[1].replaceAll(/\s+/, ' ').trim()] }.findAll{ !(it[1] =~ /(?i:duplicate)/ || it[1] =~ /\d{6,}/ || it[1].startsWith('*') || it[1].endsWith('*') || it[1].empty) } as HashSet

// join and sort
def thetvdb = thetvdb_index.findResults{ [it[0].pad(6), it[1]].join('\t') }.sort()
gz(thetvdb_out, thetvdb)
println "TheTVDB Index: " + thetvdb.size()


// BUILD series.list.gz

// TheTVDB
def thetvdb_names = thetvdb_index.findResults{ it[1] }

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




// prepare reviews from SF.net for website
def reviewPage = retry(10, 1000){ Jsoup.connect('https://sourceforge.net/projects/filebot/reviews/?sort=usefulness&filter=thumbs_up').get() }
def reviews = reviewPage.select('article[itemtype~=Review]').findResults{ article ->
	article.select('*[itemprop=reviewBody]').findAll{ !(it.attr('class') =~ /spam/) }.findResults{ review ->
		[user:article.select('*[itemprop=name]').text(), date:article.select('*[datetime]').text(), text:review.text()]
	}
}.flatten()

reviews = reviews.findAll{ it.user && !(it.date =~ /[a-z]/) }

use (groovy.json.JsonOutput) {
	println "Reviews: ${reviews.size()}"
	reviews.toJson().prettyPrint().saveAs('website/reviews.json')
}
