
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.MovieDescriptor;
import net.sourceforge.filebot.web.MovieIdentificationService;


class MovieHashMatcher implements AutoCompleteMatcher {
	
	private final MovieIdentificationService service;
	

	public MovieHashMatcher(MovieIdentificationService service) {
		this.service = service;
	}
	

	@Override
	public List<Match<File, ?>> match(final List<File> files) throws Exception {
		// handle movie files
		File[] movieFiles = filter(files, VIDEO_FILES).toArray(new File[0]);
		MovieDescriptor[] movieDescriptors = service.getMovieDescriptors(movieFiles);
		
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		for (int i = 0; i < movieDescriptors.length; i++) {
			if (movieDescriptors[i] != null) {
				matches.add(new Match<File, MovieDescriptor>(movieFiles[i], movieDescriptors[i]));
			} else {
				// unknown hash, try via imdb id from nfo file
				MovieDescriptor movie = determineMovie(movieFiles[i]);
				
				if (movie != null) {
					matches.add(new Match<File, MovieDescriptor>(movieFiles[i], movie));
				}
			}
		}
		
		// handle subtitle files
		for (File subtitle : filter(files, SUBTITLE_FILES)) {
			// check if subtitle corresponds to a movie file (same name, different extension)
			for (Match<File, ?> movieMatch : matches) {
				String subtitleName = getName(subtitle);
				String movieName = getName(movieMatch.getValue());
				
				if (subtitleName.equalsIgnoreCase(movieName)) {
					matches.add(new Match<File, Object>(subtitle, movieMatch.getCandidate()));
					// movie match found, we're done
					break;
				}
			}
		}
		
		// restore original order
		Collections.sort(matches, new Comparator<Match<File, ?>>() {
			
			@Override
			public int compare(Match<File, ?> o1, Match<File, ?> o2) {
				return files.indexOf(o1.getValue()) - files.indexOf(o2.getValue());
			}
		});
		
		return matches;
	}
	

	protected Set<Integer> grepImdbId(File... files) throws IOException {
		Set<Integer> collection = new HashSet<Integer>();
		
		for (File file : files) {
			Scanner scanner = new Scanner(file);
			
			try {
				// scan for imdb id patterns like tt1234567
				String imdb = null;
				
				while ((imdb = scanner.findWithinHorizon("(?<=tt)\\d{7}", 32 * 1024)) != null) {
					collection.add(Integer.parseInt(imdb));
				}
			} finally {
				scanner.close();
			}
		}
		
		return collection;
	}
	

	protected MovieDescriptor determineMovie(File movieFile) throws Exception {
		List<MovieDescriptor> options = new ArrayList<MovieDescriptor>();
		
		for (int imdbid : grepImdbId(movieFile.getParentFile().listFiles(getDefaultFilter("application/nfo")))) {
			MovieDescriptor movie = service.getMovieDescriptor(imdbid);
			
			if (movie != null) {
				options.add(movie);
			}
		}
		
		return options.isEmpty() ? null : selectMovie(options);
	}
	

	protected MovieDescriptor selectMovie(final List<MovieDescriptor> options) throws Exception {
		if (options.size() == 1) {
			return options.get(0);
		}
		
		// show selection dialog on EDT
		final RunnableFuture<MovieDescriptor> showSelectDialog = new FutureTask<MovieDescriptor>(new Callable<MovieDescriptor>() {
			
			@Override
			public MovieDescriptor call() throws Exception {
				// multiple results have been found, user must select one
				SelectDialog<MovieDescriptor> selectDialog = new SelectDialog<MovieDescriptor>(null, options);
				
				selectDialog.getHeaderLabel().setText("Select Movie:");
				selectDialog.getCancelAction().putValue(Action.NAME, "Ignore");
				
				// show dialog
				selectDialog.setLocation(getOffsetLocation(selectDialog.getOwner()));
				selectDialog.setVisible(true);
				
				// selected value or null if the dialog was canceled by the user
				return selectDialog.getSelectedValue();
			}
		});
		
		// allow only one select dialog at a time
		synchronized (this) {
			SwingUtilities.invokeAndWait(showSelectDialog);
		}
		
		// selected value or null
		return showSelectDialog.get();
	}
	
}
