
package net.sourceforge.filebot.ui.rename;


import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.media.ReleaseInfo;
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
	public List<Match<File, ?>> match(final List<File> files, Locale locale, boolean autodetect, Component parent) throws Exception {
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
				movie = grabMovieName(movieFiles[i], locale, autodetect, parent, movie);
				
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
				if (isDerived(getName(subtitle), movieMatch.getValue())) {
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
	
	
	protected Movie grabMovieName(File movieFile, Locale locale, boolean autodetect, Component parent, Movie... suggestions) throws Exception {
		List<Movie> options = new ArrayList<Movie>();
		
		// add default value if any
		for (Movie it : suggestions) {
			if (it != null) {
				options.add(it);
			}
		}
		
		// try to grep imdb id from nfo files
		for (int imdbid : grepImdbIdFor(movieFile)) {
			Movie movie = service.getMovieDescriptor(imdbid, locale);
			
			if (movie != null) {
				options.add(movie);
			}
		}
		
		// search by file name or folder name
		Collection<String> searchQueries = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		searchQueries.add(getName(movieFile));
		searchQueries.add(getName(movieFile.getParentFile()));
		
		// remove blacklisted terms
		searchQueries = new ReleaseInfo().cleanRG(searchQueries);
		
		for (String query : searchQueries) {
			if (autodetect && options.isEmpty()) {
				options = service.searchMovie(query, locale);
			}
		}
		
		// allow manual user input
		if (options.isEmpty() || !autodetect) {
			String suggestion = options.isEmpty() ? searchQueries.iterator().next() : options.get(0).getName();
			
			String input = null;
			synchronized (this) {
				input = showInputDialog("Enter movie name:", suggestion, searchQueries.iterator().next(), parent);
			}
			
			if (input != null) {
				options = service.searchMovie(input, locale);
			} else {
				options.clear(); // cancel search
			}
		}
		
		return options.isEmpty() ? null : selectMovie(options, parent);
	}
	
	
	protected Movie selectMovie(final List<Movie> options, final Component parent) throws Exception {
		if (options.size() == 1) {
			return options.get(0);
		}
		
		// show selection dialog on EDT
		final RunnableFuture<Movie> showSelectDialog = new FutureTask<Movie>(new Callable<Movie>() {
			
			@Override
			public Movie call() throws Exception {
				// multiple results have been found, user must select one
				SelectDialog<Movie> selectDialog = new SelectDialog<Movie>(parent, options);
				
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
