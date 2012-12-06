include('lib/ws')

def mesacc = myepisodes.split(':')
def mescheck = tryQuietly { tick } ?: 'acquired'

def mes = MyEpisodes(mesacc[0], mesacc[1])
def myshows = mes.getShowList()

def matches = { s1, s2 ->
	def norm = { s -> s.replaceAll(/\W/, '').toLowerCase() }
	return norm(s1) == norm(s2)
}

args.getFiles{ it.isVideo() && parseEpisodeNumber(it) }.groupBy{ detectSeriesName(it) }.each{ series, files ->
	def show = myshows.find{ matches(it.name, series) } ?: mes.getShows().find{ matches(it.name, series) }
	if (show != null) {
		if (!myshows.contains(show)) {
			mes.addShow(show.id)
			println "[added] $show.name"
		}
		files.each{
			def sxe = parseEpisodeNumber(it)
			mes.update(show.id, sxe.season, sxe.episode, mescheck)
			println "[$mescheck] $series $sxe [$it.name]"
		}
	} else {
		println "[failure] '$series' not found"
	}
}
