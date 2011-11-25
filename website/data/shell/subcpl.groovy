// filebot -script "http://filebot.sourceforge.net/data/shell/subcpl.groovy" <options> <folder>

import static net.sourceforge.tuned.FileUtilities.*;


/*
 * Fetch subtitles for all videos that currently don't have subtitles
 */
args.eachMediaFolder { dir ->
	// select videos without subtitles
	def videos = dir.listFiles().findAll{ video ->
		video.isVideo() && !dir.listFiles().find{ sub ->
			sub.isSubtitle() && isDerived(sub,video)
		}
	}
	
	// fetch subtitles by hash only
	getSubtitles(file:videos, strict:true)
}
