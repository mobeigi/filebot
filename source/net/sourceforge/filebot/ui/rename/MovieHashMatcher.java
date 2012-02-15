
package net.sourceforge.filebot.ui.rename;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Component;
import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.media.ReleaseInfo;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MoviePart;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.tuned.FileUtilities.FolderFilter;


class MovieHashMatcher implements AutoCompleteMatcher {
	
	private final MovieIdentificationService service;
	
	
	public MovieHashMatcher(MovieIdentificationService service) {
		this.service = service;
	}
	
	
	@Override
	public List<Match<File, ?>> match(final List<File> files, final SortOrder sortOrder, final Locale locale, final boolean autodetect, final Component parent) throws Exception {
		// handle movie files
		List<File> movieFiles = filter(files, VIDEO_FILES);
		List<File> nfoFiles = filter(files, MediaTypes.getDefaultFilter("application/nfo"));
		
		List<File> orphanedFiles = new ArrayList<File>(filter(files, FILES));
		orphanedFiles.removeAll(movieFiles);
		orphanedFiles.removeAll(nfoFiles);
		
		Map<File, List<File>> derivatesByMovieFile = new HashMap<File, List<File>>();
		for (File movieFile : movieFiles) {
			derivatesByMovieFile.put(movieFile, new ArrayList<File>());
		}
		for (File file : orphanedFiles) {
			for (File movieFile : movieFiles) {
				if (isDerived(file, movieFile)) {
					derivatesByMovieFile.get(movieFile).add(file);
					break;
				}
			}
		}
		for (List<File> derivates : derivatesByMovieFile.values()) {
			orphanedFiles.removeAll(derivates);
		}
		
		// match movie hashes online
		final Map<File, Movie> movieByFile = new HashMap<File, Movie>();
		if (movieFiles.size() > 0) {
			try {
				Map<File, Movie> hashLookup = service.getMovieDescriptors(movieFiles, locale);
				movieByFile.putAll(hashLookup);
				Analytics.trackEvent(service.getName(), "HashLookup", "Movie", hashLookup.size()); // number of positive hash lookups
			} catch (UnsupportedOperationException e) {
				// ignore
			}
		}
		for (File nfo : nfoFiles) {
			try {
				Movie movie = grepMovie(nfo, service, locale);
				movieByFile.put(nfo, movie);
				
				// match movie info to movie files that match the nfo file name
				SortedSet<File> siblingMovieFiles = new TreeSet<File>(filter(movieFiles, new FolderFilter(nfo.getParentFile())));
				for (File movieFile : siblingMovieFiles) {
					if (isDerived(movieFile, nfo)) {
						movieByFile.put(movieFile, movie);
					}
				}
			} catch (NoSuchElementException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to grep IMDbID: " + nfo.getName());
			}
		}
		
		// collect files that will be matched one by one
		List<File> movieMatchFiles = new ArrayList<File>();
		movieMatchFiles.addAll(movieFiles);
		movieMatchFiles.addAll(nfoFiles);
		movieMatchFiles.addAll(filter(files, new ReleaseInfo().getDiskFolderFilter()));
		movieMatchFiles.addAll(filter(orphanedFiles, SUBTITLE_FILES)); // run movie detection only on orphaned subtitle files
		
		// match remaining movies file by file in parallel
		List<Callable<Entry<File, Movie>>> grabMovieJobs = new ArrayList<Callable<Entry<File, Movie>>>();
		
		// map all files by movie
		for (final File file : movieMatchFiles) {
			grabMovieJobs.add(new Callable<Entry<File, Movie>>() {
				
				@Override
				public Entry<File, Movie> call() throws Exception {
					// unknown hash, try via imdb id from nfo file
					if (!movieByFile.containsKey(file) || !autodetect) {
						Movie result = grabMovieName(file, locale, autodetect, parent, movieByFile.get(file));
						if (result != null) {
							Analytics.trackEvent(service.getName(), "SearchMovie", result.toString(), 1);
						}
						return new SimpleEntry<File, Movie>(file, result);
					}
					return new SimpleEntry<File, Movie>(file, movieByFile.get(file));
				}
			});
		}
		
		// map movies to (possibly multiple) files (in natural order) 
		Map<Movie, SortedSet<File>> filesByMovie = new HashMap<Movie, SortedSet<File>>();
		
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		try {
			for (Future<Entry<File, Movie>> it : executor.invokeAll(grabMovieJobs)) {
				// check if we managed to lookup the movie descriptor
				File file = it.get().getKey();
				Movie movie = it.get().getValue();
				
				// get file list for movie
				if (movie != null) {
					SortedSet<File> movieParts = filesByMovie.get(movie);
					if (movieParts == null) {
						movieParts = new TreeSet<File>();
						filesByMovie.put(movie, movieParts);
					}
					movieParts.add(file);
				}
			}
		} finally {
			executor.shutdown();
		}
		
		// collect all File/MoviePart matches
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		for (Entry<Movie, SortedSet<File>> entry : filesByMovie.entrySet()) {
			for (List<File> fileSet : mapByExtension(entry.getValue()).values()) {
				// resolve movie parts
				for (int i = 0; i < fileSet.size(); i++) {
					Movie moviePart = entry.getKey();
					if (fileSet.size() > 1) {
						moviePart = new MoviePart(moviePart, i + 1, fileSet.size());
					}
					
					matches.add(new Match<File, Movie>(fileSet.get(i), moviePart));
					
					// automatically add matches for derivate files
					List<File> derivates = derivatesByMovieFile.get(fileSet.get(i));
					if (derivates != null) {
						for (File derivate : derivates) {
							matches.add(new Match<File, Movie>(derivate, moviePart));
						}
					}
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
		Set<Movie> options = new LinkedHashSet<Movie>();
		
		// add default value if any
		for (Movie it : suggestions) {
			if (it != null) {
				options.add(it);
			}
		}
		
		// auto-detect movie from nfo or folder / file name
		options.addAll(detectMovie(movieFile, null, service, locale, false));
		
		// allow manual user input
		if (options.isEmpty() || !autodetect) {
			String suggestion = options.isEmpty() ? stripReleaseInfo(getName(movieFile)) : options.iterator().next().getName();
			
			String input = null;
			synchronized (this) {
				input = showInputDialog("Enter movie name:", suggestion, movieFile.getPath(), parent);
			}
			
			// we only care about results from manual input from here on out
			options.clear();
			
			if (input != null) {
				options.addAll(service.searchMovie(input, locale));
			}
		}
		
		return options.isEmpty() ? null : selectMovie(movieFile, options, parent);
	}
	
	
	protected Movie selectMovie(final File movieFile, final Collection<Movie> options, final Component parent) throws Exception {
		if (options.size() == 1) {
			return options.iterator().next();
		}
		
		// auto-select most probable search result
		final List<Movie> probableMatches = new LinkedList<Movie>();
		
		// use name similarity metric
		final String query = stripReleaseInfo(getName(movieFile));
		final SimilarityMetric metric = new NameSimilarityMetric();
		
		// find probable matches using name similarity >= 0.9
		for (Movie result : options) {
			if (metric.getSimilarity(query, result.getName()) >= 0.9) {
				probableMatches.add(result);
			}
		}
		
		// auto-select first and only probable search result
		if (probableMatches.size() == 1) {
			return probableMatches.get(0);
		}
		
		// show selection dialog on EDT
		final RunnableFuture<Movie> showSelectDialog = new FutureTask<Movie>(new Callable<Movie>() {
			
			@Override
			public Movie call() throws Exception {
				// multiple results have been found, user must select one
				SelectDialog<Movie> selectDialog = new SelectDialog<Movie>(parent, options);
				
				selectDialog.setTitle(movieFile.getPath());
				selectDialog.getHeaderLabel().setText(String.format("Movies matching '%s':", query));
				selectDialog.getCancelAction().putValue(Action.NAME, "Ignore");
				selectDialog.pack();
				
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
