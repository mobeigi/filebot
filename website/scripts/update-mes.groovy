// filebot -script fn:update-mes "X:/path/to/episodes" --def login=user:pwd addshows=y tick=acquired

def mesacc = login.split(':')
def mesadd = tryQuietly{ addshows.toBoolean() }
def mesupdate = tryQuietly { tick } ?: 'acquired'
def mesvalue = tryQuietly { value } ?: '1'

// import myepisodes scraper
include('lib/scraper')

def mes = MyEpisodes(mesacc[0], mesacc[1])
def myshows = mes.getShowList()

// series name => series key (e.g. Doctor Who (2005) => doctorwho)
def collationKey = { s -> s == null ? '' : s.replaceAll(/^(?i)(The|A)\b/, '').replaceAll(/(?<!\d)\d{4}$/).replaceAll(/\W/).lower() }

args.getFiles().findAll{ it.isVideo() && parseEpisodeNumber(it) && detectSeriesName(it) }.groupBy{ detectSeriesName(it) }.each{ series, files ->
	def show = myshows.find{ collationKey(it.name) == collationKey(series) }
	if (show == null && mesadd) {
		show = mes.getShows().find{ collationKey(it.name) == collationKey(series) }
		if (show == null) {
			println "[failure] '$series' not found"
			return
		}
		mes.addShow(show.id)
		println "[added] $show.name"
	}
	
	files.each{
		if (show != null) {
			def sxe = parseEpisodeNumber(it)
			mes.update(show.id, sxe.season, sxe.episode, mesupdate, mesvalue)
			println "[$mesupdate] $show.name $sxe [$it.name]"
		} else {
			println "[failure] '$series' has not been added [$it.name]"
		}
	}
}
