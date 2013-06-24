// OpenSubtitles
console.print('Enter OpenSubtitles username: ')
def osdbUser = console.readLine()
console.print('Enter OpenSubtitles password: ')
def osdbPwd = console.readLine()


setLogin('osdb.user', osdbUser, osdbPwd)


/* --------------------------------------------------------------------- */

import net.sourceforge.filebot.*

if (osdbUser) {
	console.print('Testing OpenSubtitles... ')
	WebServices.OpenSubtitles.setUser(osdbUser, osdbPwd)
	WebServices.OpenSubtitles.login()
	console.println('OK')
}

/* --------------------------------------------------------------------- */

def setLogin(key, user, pwd) {
	Settings.forPackage(WebServices.class).put(key, [user, pwd].join(':'))
}
