// filebot -script fn:housekeeping /path/to/folder/ --output /output/folder/ --format <expression>

/*
 * Watch folder for new tv shows and automatically move/rename new episodes
 */

// check for new media files once every 5 minutes
def updateFrequency = 5 * 60 * 1000

// spawn daemon thread
Thread.startDaemon {
	while (sleep(updateFrequency) || true) {
		// extract all
		if (_args.extract) {
			extract(file:args.getFiles{ it.isArchive() }, output:'.')
		}
		
		// subtitles for all
		if (_args.getSubtitles) {
			getMissingSubtitles(file:args.getFiles{ it.isVideo() }, output:'srt')
		}
			
		// rename all
		if (_args.rename) {
			args.eachMediaFolder {
				rename(folder:it)
			}
		}
	}
}

println "Press ENTER to abort"
console.readLine() // keep script running until aborted by user
