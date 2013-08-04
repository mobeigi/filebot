/****************************************************************************
 * Pushover
 * 				https://pushover.net
 ****************************************************************************/
def Pushover(user, token = 'wcckDz3oygHSU2SdIptvnHxJ92SQKK') {
	new PushoverClient(user:user, token:token)
}

class PushoverClient {
	def user
	def token
	
	def endpoint =  new URL('https://api.pushover.net/1/messages.xml')
	
	def send = { text, parameters = [:] ->
		// inject default post parameters
		parameters << [token:token, user:user, message:text as String]
		
		// post and process response
		endpoint.post(parameters).text.xml
	}
}
