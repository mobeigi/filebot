
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
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
	public List<Match<File, ?>> match(final List<File> files, Locale locale) throws Exception {
		// handle movie files
		File[] movieFiles = filter(files, VIDEO_FILES).toArray(new File[0]);
		MovieDescriptor[] movieDescriptors = service.getMovieDescriptors(movieFiles, locale);
		
		// map movies to (possibly multiple) files (in natural order) 
		Map<MovieDescriptor, SortedSet<File>> filesByMovie = new HashMap<MovieDescriptor, SortedSet<File>>();
		
		// map all files by movie
		for (int i = 0; i < movieFiles.length; i++) {
			MovieDescriptor movie = movieDescriptors[i];
			
			// unknown hash, try via imdb id from nfo file
			if (movie == null) {
				movie = determineMovie(movieFiles[i], locale);
			}
			
			// check if we managed to lookup the movie descriptor
			if (movie != null) {
				// get file list for movie
				SortedSet<File> movieParts = filesByMovie.get(movie);
				
				if (movieParts == null) {
					movieParts = new TreeSet<File>();
					filesByMovie.put(movie, movieParts);
				}
				
				movieParts.add(movieFiles[i]);
			}
		}
		
		// collect all File/MoviePart matches
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		for (Entry<MovieDescriptor, SortedSet<File>> entry : filesByMovie.entrySet()) {
			MovieDescriptor movie = entry.getKey();
			
			int partIndex = 0;
			int partCount = entry.getValue().size();
			
			// add all movie parts
			for (File file : entry.getValue()) {
				matches.add(new Match<File, MoviePart>(file, new MoviePart(movie, partIndex++, partCount)));
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
	

	protected MovieDescriptor determineMovie(File movieFile, Locale locale) throws Exception {
		List<MovieDescriptor> options = new ArrayList<MovieDescriptor>();
		
		// try to grep imdb id from nfo files
		for (int imdbid : grepImdbId(movieFile.getParentFile().listFiles(getDefaultFilter("application/nfo")))) {
			MovieDescriptor movie = service.getMovieDescriptor(imdbid, locale);
			
			if (movie != null) {
				options.add(movie);
			}
		}
		
		// search by file name
		if (options.isEmpty()) {
			String query = getName(movieFile).replaceAll("\\p{Punct}+", " ").trim();
			options = service.searchMovie(query, locale);
			
			// search by folder name
			if (options.isEmpty()) {
				query = getName(movieFile.getParentFile()).replaceAll("\\p{Punct}+", " ").trim();
				options = service.searchMovie(query, locale);
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
