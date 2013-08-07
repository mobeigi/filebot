/****************************************************************************
 * MyEpisodes
 * 				http://www.myepisodes.com
 ****************************************************************************/
import org.jsoup.Jsoup
import org.jsoup.Connection.Method
import net.sourceforge.filebot.Cache

def MyEpisodes(username, password) {
	return new MyEpisodesScraper(username:username, password:password)
}

class MyEpisodesScraper {
	def username
	def password
	
	def cache = Cache.getCache('web-datasource-lv2')
	def session = [:]
	
	def login = {
		def response = Jsoup.connect('http://www.myepisodes.com/login.php').data('username', username, 'password', password, 'action', 'Login', 'u', '').method(Method.POST).execute()
		session << response.cookies()
		return response.parse()
	}
	
	def get = { url ->
		if (session.isEmpty()) {
			login()
		}
		
		def response = Jsoup.connect(url).cookies(session).method(Method.GET).execute()
		session << response.cookies()
		def html = response.parse()
		
		if (html.select('#frmLogin')) {
			session.clear()
			throw new Exception('Login failed')
		}
		
		return html
	}
	
	def getShows = {
		def shows = cache.get('MyEpisodes.Shows')
		if (shows == null) {
			shows = ['other', 'A'..'Z'].flatten().findResults{ section ->
				get("http://myepisodes.com/shows.php?list=${section}").select('a').findResults{ a ->
					try {
						return [id:a.absUrl('href').match(/showid=(\d+)/).toInteger(), name:a.text().trim()]
					} catch(e) {
						return null
					}
				}
			}.flatten().sort{ it.name }
			cache.put('MyEpisodes.Shows', shows)
		}
		return shows
	}
	
	def getShowList = {
		get("http://www.myepisodes.com/shows.php?type=manage").select('option').findResults{ option ->
			try {
				return [id:option.attr('value').toInteger(), name:option.text().trim()]
			} catch(e) {
				return null
			}
		}
	}
	
	def addShow = { showid ->
		get("http://www.myepisodes.com/views.php?type=manageshow&mode=add&showid=${showid}")
	}
	
	def update = { showid, season, episode, tick = 'acquired', value = '1' ->
		get("http://www.myepisodes.com/myshows.php?action=Update&showid=${showid}&season=${season}&episode=${episode}&${tick}=${value}")
	}
	
}
