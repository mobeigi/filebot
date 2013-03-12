// OpenSubtitles
console.print('Enter OpenSubtitles username: ')
def osdbUser = console.readLine()
console.print('Enter OpenSubtitles password: ')
def osdbPwd = console.readLine()

// Sublight
console.print('Enter Sublight username: ')
def sublightUser = console.readLine()
console.print('Enter Sublight password: ')
def sublightPwd = console.readLine()

// I've requested a new API key for FileBot multiple times and have yet to recieve a reply...
// @from http://sublightcmd.codeplex.com/SourceControl/changeset/view/9437#8043
def sublightClientId = 'SublightCmd'
def sublightApiKey = '12c72276-b95f-4144-bb2a-879775c71437'


setLogin('osdb.user', osdbUser, osdbPwd)
setLogin('sublight.client', sublightClientId, sublightApiKey)
setLogin('sublight.user', sublightUser, sublightPwd)


/* --------------------------------------------------------------------- */

import net.sourceforge.filebot.*

if (osdbUser) {
	console.print('Testing OpenSubtitles... ')
	WebServices.OpenSubtitles.setUser(osdbUser, osdbPwd)
	WebServices.OpenSubtitles.login()
	console.println('OK')
}

if (sublightUser) {
	console.print('Testing Sublight... ')
	WebServices.Sublight.setClient(sublightClientId, sublightApiKey)
	WebServices.Sublight.setUser(sublightUser, sublightPwd)
	WebServices.Sublight.getSubtitleList(null, 'Avatar', 2009, 'English')
	console.println('OK')
}

/* --------------------------------------------------------------------- */

def setLogin(key, user, pwd) {
	Settings.forPackage(WebServices.class).put(key, [user, pwd].join(':'))
}
