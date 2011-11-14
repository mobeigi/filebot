
package net.sourceforge.filebot.ui.rename;


import static java.util.Arrays.*;
import static java.util.Collections.*;
import static javax.swing.JOptionPane.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MoviePart;


class MovieHashMatcher implements AutoCompleteMatcher {
	
	private final MovieIdentificationService service;
	

	public MovieHashMatcher(MovieIdentificationService service) {
		this.service = service;
	}
	

	@Override
	public List<Match<File, ?>> match(final List<File> files, Locale locale, boolean autodetect) throws Exception {
		// handle movie files
		File[] movieFiles = filter(files, VIDEO_FILES).toArray(new File[0]);
		
		// match movie hashes online
		Movie[] movieByFileHash = service.getMovieDescriptors(movieFiles, locale);
		Analytics.trackEvent(service.getName(), "HashLookup", "Movie", movieByFileHash.length - frequency(asList(movieByFileHash), null)); // number of positive hash lookups
		
		// map movies to (possibly multiple) files (in natural order) 
		Map<Movie, SortedSet<File>> filesByMovie = new HashMap<Movie, SortedSet<File>>();
		
		// map all files by movie
		for (int i = 0; i < movieFiles.length; i++) {
			Movie movie = movieByFileHash[i];
			
			// unknown hash, try via imdb id from nfo file
			if (movie == null || !autodetect) {
				movie = grabMovieName(movieFiles[i], locale, autodetect, movie);
				
				if (movie != null) {
					Analytics.trackEvent(service.getName(), "SearchMovie", movie.toString(), 1);
				}
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
		
		for (Entry<Movie, SortedSet<File>> entry : filesByMovie.entrySet()) {
			Movie movie = entry.getKey();
			
			int partIndex = 0;
			int partCount = entry.getValue().size();
			
			// add all movie parts
			for (File file : entry.getValue()) {
				Movie part = movie;
				if (partCount > 1) {
					part = new MoviePart(movie, ++partIndex, partCount);
				}
				
				matches.add(new Match<File, Movie>(file, part));
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
	

	private Set<Integer> grepImdbId(File... files) throws IOException {
		Set<Integer> collection = new HashSet<Integer>();
		
		for (File file : files) {
			Scanner scanner = new Scanner(new FileInputStream(file), "UTF-8");
			
			try {
				// scan for imdb id patterns like tt1234567
				String imdb = null;
				
				while ((imdb = scanner.findWithinHorizon("(?<=tt)\\d{7}", 64 * 1024)) != null) {
					collection.add(Integer.parseInt(imdb));
				}
			} finally {
				scanner.close();
			}
		}
		
		return collection;
	}
	

	private String normalizeMovieName(File movie) {
		return getName(movie).replaceAll("\\p{Punct}+", " ").trim();
	}
	

	protected Movie grabMovieName(File movieFile, Locale locale, boolean autodetect, Movie... suggestions) throws Exception {
		List<Movie> options = new ArrayList<Movie>();
		
		// add default value if any
		for (Movie it : suggestions) {
			if (it != null) {
				options.add(it);
			}
		}
		
		// try to grep imdb id from nfo files
		for (int imdbid : grepImdbId(movieFile.getParentFile().listFiles(getDefaultFilter("application/nfo")))) {
			Movie movie = service.getMovieDescriptor(imdbid, locale);
			
			if (movie != null) {
				options.add(movie);
			}
		}
		
		// search by file name or folder name
		Set<String> searchQueries = new LinkedHashSet<String>(2);
		searchQueries.add(normalizeMovieName(movieFile));
		searchQueries.add(normalizeMovieName(movieFile.getParentFile()));
		
		for (String query : searchQueries) {
			if (autodetect && options.isEmpty()) {
				options = service.searchMovie(query, locale);
			}
		}
		
		// allow manual user input
		if (options.isEmpty() || !autodetect) {
			String suggestion = options.isEmpty() ? normalizeMovieName(movieFile) : options.get(0).getName();
			String input = showInputDialog(null, "Enter movie name:", suggestion);
			
			if (input != null) {
				options = service.searchMovie(input, locale);
			} else {
				options.clear(); // cancel search
			}
		}
		
		return options.isEmpty() ? null : selectMovie(options);
	}
	

	protected Movie selectMovie(final List<Movie> options) throws Exception {
		if (options.size() == 1) {
			return options.get(0);
		}
		
		// show selection dialog on EDT
		final RunnableFuture<Movie> showSelectDialog = new FutureTask<Movie>(new Callable<Movie>() {
			
			@Override
			public Movie call() throws Exception {
				// multiple results have been found, user must select one
				SelectDialog<Movie> selectDialog = new SelectDialog<Movie>(null, options);
				
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
