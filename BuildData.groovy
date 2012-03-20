// filebot -script BuildData.groovy -trust-script

def s_out = new File("website/data/series.list.gz")
def m_out = new File("website/data/movies.txt.gz")

def gz(file, lines) {
	file.withOutputStream{ out ->
		new java.util.zip.GZIPOutputStream(out).withWriter('utf-8'){ writer ->
			lines.each{ writer.append(it).append('\n') }
		}
	}
}


// ------------------------------------------------------------------------- //


// BUILD movies.txt.gz
def tsv = new URL("http://www.opensubtitles.org/addons/export_movie.php")
def movies = []

tsv.text.eachLine{
	def line = it.split(/\t/)*.replaceAll(/\s+/, ' ')*.trim()
	if (line.size() == 4 && line[0] =~ /\d+/) {
		movies.add([line[1].toInteger(), line[2], line[3].toInteger()])
	}
}

movies = movies.findAll{ it[0] <= 9999999 && it[2] >= 1960 && it[1] =~ /^[A-Z0-9]/ && it[1] =~ /[\p{Alpha}]{3}/ }.sort{ it[1] }

gz(m_out, movies.collect{ [it[0].pad(7), it[1], it[2]].join('\t') })
println "Movie Count: " + movies.size()


// ------------------------------------------------------------------------- //


// BUILD series.list.gz
def page = new URL('http://thetvdb.com/?string=&searchseriesid=&tab=listseries&function=Search')

def names = page.fetch().getHtml('utf-8')
.depthFirst().TABLE.find{it['@id'] == "listtable"}
.depthFirst().TR.findAll{ it.TD.size() == 3 && it.TD[1].text() == 'English'}
.findResults{ it.TD[0].A.text() }

if (names.size() == 0) {
	throw new Exception("Failed to scrape series names")
}

def anime = net.sourceforge.filebot.WebServices.AniDB.getAnimeTitles()
names += anime.findResults{ it.getPrimaryTitle() }
names += anime.findResults{ it.getOfficialTitle('en') }

names = names.findAll{ it =~ /^[A-Z0-9]/ && it =~ /[\p{Alpha}]{3}/}.findResults{ net.sourceforge.filebot.similarity.Normalization.normalizePunctuation(it) }

def unique = new TreeSet(String.CASE_INSENSITIVE_ORDER)
unique.addAll(names)
names = unique as List


gz(s_out, names)
println "Series Count: " + names.size()
