def page = new URL('http://thetvdb.com/?string=&searchseriesid=&tab=listseries&function=Search')

def names = page.fetch().getHtml('utf-8')
.depthFirst().TABLE.find{it['@id'] == "listtable"}
.depthFirst().TR.findAll{ it.TD.size() == 3 && it.TD[1].text() == 'English'}
.findResults{ it.TD[0].A.text() }

def anime = net.sourceforge.filebot.WebServices.AniDB.getAnimeTitles()
names += anime.findResults{ it.getPrimaryTitle() }
names += anime.findResults{ it.getOfficialTitle('en') }

names = names.findAll{ it =~ /^[A-Z]/ && it =~ /[\p{Alpha}]{3}/}.findResults{ net.sourceforge.filebot.similarity.Normalization.normalizePunctuation(it) }
names = names.sort().unique()


args[0].withOutputStream{ out ->
	new java.util.zip.GZIPOutputStream(out).withWriter('utf-8'){ writer ->
		names.each{ writer.append(it).append('\n') }
	}
}

println "Series Count: " + names.size()
