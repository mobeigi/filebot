// filebot -script "http://filebot.sourceforge.net/data/shell/housekeeping.groovy" <folder>

/*
* Watch folder for new tv shows and automatically 
* move/rename new episodes into a predefined folder structure
*/

// check for new media files once every 5 seconds
def updateFrequency = 5 * 1000;

// V:/path for windows /usr/home/name/ for unix
def destinationRoot = "{com.sun.jna.Platform.isWindows() ? file.path[0..1] : System.getProperty('user.home')}"

// V:/TV Shows/Stargate/Season 1/Stargate.S01E01.Pilot
def episodeFormat = destinationRoot + "/TV Shows/{n}{'/Season '+s}/{n.space('.')}.{s00e00}.{t.space('.')}"

// spawn daemon thread
Thread.startDaemon {
	while (sleep(updateFrequency) || true) {
		args.eachMediaFolder {
			rename(folder:it, db: "thetvdb", format:episodeFormat)
		}
	}
}

println "Press ENTER to abort"
console.readLine() // keep script running until aborted by user
