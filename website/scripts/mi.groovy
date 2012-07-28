// filebot -script fn:mi /path/to/media/ "MediaIndex.csv"

/*
 * Print media info of all video files to CSV file  
 */
def model = 'Name;Container;Resolution;Video Codec;Video Format;Audio Codec;Audio Format;Audio Language(s);Duration;File Size;Path'
def template = '{fn};{cf};{resolution};{vc};{vf};{ac};{af};{media.AudioLanguageList};{media.DurationString3};{file.length()};{file.getCanonicalPath()}'

// open destination file (writing files requires -trust-script)
args[1].withWriter{ output ->
	// print header
	output.writeLine(model)
	
	// print info for each video file (sorted by filename)
	args[0].getFiles{ it.isVideo() }.sort{ a, b -> a.name.compareToIgnoreCase(b.name) }.each{
		def mi = getMediaInfo(file:it, format:template)
		
		// print to console
		println mi
		
		// append to file
		output.writeLine(mi)
	}
}
