
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
			} else {
				// unknown hash, try via imdb id from nfo file
				MovieDescriptor movie = determineMovie(movieFiles[i]);
				
				if (movie != null) {
					matches.add(new Match<File, MovieDescriptor>(movieFiles[i], movie));
				}
			}
		}
		
		return matches;
	}
	

	protected Set<Integer> grepImdbId(File... files) throws IOException {
		Set<Integer> collection = new HashSet<Integer>();
		
		for (File file : files) {
			Scanner scanner = new Scanner(file);
			String imdb = null;
			
			// scan for imdb id patterns like tt1234567
			while ((imdb = scanner.findWithinHorizon("(?<=tt)\\d{7}", 32 * 1024)) != null) {
				collection.add(Integer.parseInt(imdb));
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
