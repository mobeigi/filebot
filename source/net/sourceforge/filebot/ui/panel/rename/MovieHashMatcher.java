
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.MediaTypes.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.MovieDescriptor;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.tuned.FileUtilities;


class MovieHashMatcher implements AutoCompleteMatcher {
	
	private final MovieIdentificationService service;
	

	public MovieHashMatcher(MovieIdentificationService service) {
		this.service = service;
	}
	

	@Override
	public List<Match<File, ?>> match(List<File> files) throws Exception {
		// focus on movie and subtitle files
		File[] movieFiles = FileUtilities.filter(files, VIDEO_FILES).toArray(new File[0]);
		MovieDescriptor[] movieDescriptors = service.getMovieDescriptors(movieFiles);
		
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		for (int i = 0; i < movieDescriptors.length; i++) {
			if (movieDescriptors[i] != null) {
				matches.add(new Match<File, MovieDescriptor>(movieFiles[i], movieDescriptors[i]));
			}
		}
		
		return matches;
	}
	
}
