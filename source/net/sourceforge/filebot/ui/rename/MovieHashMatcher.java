package net.sourceforge.filebot.ui.rename;

import static java.awt.Cursor.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.filebot.similarity.CommonSequenceMatcher.*;
import static net.sourceforge.filebot.similarity.Normalization.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MoviePart;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.tuned.FileUtilities.ParentFilter;

class MovieHashMatcher implements AutoCompleteMatcher {

	private final MovieIdentificationService service;

	public MovieHashMatcher(MovieIdentificationService service) {
		this.service = service;
	}

	@Override
	public List<Match<File, ?>> match(final List<File> files, final SortOrder sortOrder, final Locale locale, final boolean autodetect, final Component parent) throws Exception {
		if (files.isEmpty()) {
			return justFetchMovieInfo(locale, parent);
		}

		// ignore sample files
		List<File> fileset = filter(files, not(getClutterFileFilter()));

		// handle movie files
		Set<File> movieFiles = new TreeSet<File>(filter(fileset, VIDEO_FILES));
		Set<File> nfoFiles = new TreeSet<File>(filter(fileset, NFO_FILES));

		List<File> orphanedFiles = new ArrayList<File>(filter(fileset, FILES));
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
		final Map<File, Movie> movieByFile = new TreeMap<File, Movie>();
		if (autodetect && movieFiles.size() > 0) {
			try {
				Map<File, Movie> hashLookup = service.getMovieDescriptors(movieFiles, locale);
				movieByFile.putAll(hashLookup);
				Analytics.trackEvent(service.getName(), "HashLookup", "Movie", hashLookup.size()); // number of positive hash lookups
			} catch (UnsupportedOperationException e) {
				// ignore
			}
		}

		// collect useful nfo files even if they are not part of the selected fileset
		Set<File> effectiveNfoFileSet = new TreeSet<File>(nfoFiles);
		for (File dir : mapByFolder(movieFiles).keySet()) {
			addAll(effectiveNfoFileSet, dir.listFiles(NFO_FILES));
		}
		for (File dir : filter(fileset, FOLDERS)) {
			addAll(effectiveNfoFileSet, dir.listFiles(NFO_FILES));
		}

		for (File nfo : effectiveNfoFileSet) {
			try {
				Movie movie = grepMovie(nfo, service, locale);

				// ignore illegal nfos
				if (movie == null) {
					continue;
				}

				if (nfoFiles.contains(nfo)) {
					movieByFile.put(nfo, movie);
				}

				if (isDiskFolder(nfo.getParentFile())) {
					// special handling for disk folders
					for (File folder : fileset) {
						if (nfo.getParentFile().equals(folder)) {
							movieByFile.put(folder, movie);
						}
					}
				} else {
					// match movie info to movie files that match the nfo file name
					SortedSet<File> siblingMovieFiles = new TreeSet<File>(filter(movieFiles, new ParentFilter(nfo.getParentFile())));
					String baseName = stripReleaseInfo(getName(nfo)).toLowerCase();

					for (File movieFile : siblingMovieFiles) {
						if (!baseName.isEmpty() && stripReleaseInfo(getName(movieFile)).toLowerCase().startsWith(baseName)) {
							movieByFile.put(movieFile, movie);
						}
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
		movieMatchFiles.addAll(filter(files, FOLDERS));
		movieMatchFiles.addAll(filter(orphanedFiles, SUBTITLE_FILES)); // run movie detection only on orphaned subtitle files

		// match remaining movies file by file in parallel
		List<Future<Map<File, Collection<Movie>>>> grabMovieJobs = new ArrayList<Future<Map<File, Collection<Movie>>>>();

		// process in parallel
		ExecutorService executor = Executors.newFixedThreadPool(getPreferredThreadPoolSize());

		// map all files by movie
		List<File> remainingFiles = new ArrayList<File>();

		for (File file : movieMatchFiles) {
			if (movieByFile.get(file) == null) {
				remainingFiles.add(file);
			}
		}

		for (final Collection<File> folder : mapByFolder(remainingFiles).values()) {
			grabMovieJobs.add(executor.submit(new Callable<Map<File, Collection<Movie>>>() {

				@Override
				public Map<File, Collection<Movie>> call() throws Exception {
					Map<File, Collection<Movie>> detection = new LinkedHashMap<File, Collection<Movie>>();
					for (File f : folder) {
						detection.put(f, detectMovie(f, null, service, locale, false));
					}
					return detection;
				}
			}));
		}

		// remember user decisions and only bother user once
		Map<String, Object> memory = new HashMap<String, Object>();
		memory.put("input", new TreeMap<String, String>(getLenientCollator(locale)));
		memory.put("selection", new TreeMap<String, String>(getLenientCollator(locale)));

		try {
			for (Future<Map<File, Collection<Movie>>> detection : grabMovieJobs) {
				// auto-select movie or ask user
				for (Entry<File, Collection<Movie>> it : detection.get().entrySet()) {
					File movieFile = it.getKey();
					Movie movie = grabMovieName(movieFile, it.getValue(), locale, autodetect, memory, parent);
					if (movie != null) {
						movieByFile.put(movieFile, movie);
					}
				}
			}
		} finally {
			executor.shutdownNow();
		}

		// map movies to (possibly multiple) files (in natural order)
		Map<Movie, SortedSet<File>> filesByMovie = new HashMap<Movie, SortedSet<File>>();

		// collect movie part data
		for (Entry<File, Movie> it : movieByFile.entrySet()) {
			SortedSet<File> movieParts = filesByMovie.get(it.getValue());
			if (movieParts == null) {
				movieParts = new TreeSet<File>();
				filesByMovie.put(it.getValue(), movieParts);
			}
			movieParts.add(it.getKey());
		}

		// collect all File/MoviePart matches
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();

		for (Entry<Movie, SortedSet<File>> byMovie : filesByMovie.entrySet()) {
			for (List<File> movieFileListByMediaFolder : mapByMediaFolder(byMovie.getValue()).values()) {
				for (List<File> fileSet : mapByExtension(movieFileListByMediaFolder).values()) {
					// resolve movie parts
					for (int i = 0; i < fileSet.size(); i++) {
						Movie moviePart = byMovie.getKey();
						if (fileSet.size() > 1) {
							moviePart = new MoviePart(moviePart, i + 1, fileSet.size());
						}

						matches.add(new Match<File, Movie>(fileSet.get(i), moviePart.clone()));

						// automatically add matches for derived files
						List<File> derivates = derivatesByMovieFile.get(fileSet.get(i));
						if (derivates != null) {
							for (File derivate : derivates) {
								matches.add(new Match<File, Movie>(derivate, moviePart.clone()));
							}
						}
					}
				}
			}
		}

		// restore original order
		sort(matches, new Comparator<Match<File, ?>>() {

			@Override
			public int compare(Match<File, ?> o1, Match<File, ?> o2) {
				return files.indexOf(o1.getValue()) - files.indexOf(o2.getValue());
			}
		});

		return matches;
	}

	protected Movie grabMovieName(File movieFile, Collection<Movie> options, Locale locale, boolean autodetect, Map<String, Object> memory, Component parent) throws Exception {
		// allow manual user input
		if (!autodetect || options.isEmpty()) {
			if (autodetect && memory.containsKey("repeat")) {
				return null;
			}

			String suggestion = options.isEmpty() ? stripReleaseInfo(getName(movieFile)) : options.iterator().next().getName();

			@SuppressWarnings("unchecked")
			Map<String, String> inputMemory = (Map<String, String>) memory.get("input");

			String input = inputMemory.get(suggestion);
			if (input == null || suggestion == null || suggestion.isEmpty()) {
				File movieFolder = guessMovieFolder(movieFile);
				input = showInputDialog("Enter movie name:", suggestion != null && suggestion.length() > 0 ? suggestion : getName(movieFile), movieFolder == null ? movieFile.getName() : String.format("%s/%s", movieFolder.getName(), movieFile.getName()), parent);
				inputMemory.put(suggestion, input);
			}

			if (input != null) {
				options = service.searchMovie(input, locale);
				if (options.size() > 0) {
					return selectMovie(movieFile, input, options, memory, parent);
				}
			}
		}

		return options.isEmpty() ? null : selectMovie(movieFile, null, options, memory, parent);
	}

	protected String checkedStripReleaseInfo(File file) throws Exception {
		String name = stripReleaseInfo(getName(file));

		// try to redeem possible false negative matches
		if (name.length() < 2) {
			Movie match = checkMovie(file, false);
			if (match != null) {
				return match.getName();
			}
		}

		return name;
	}

	protected Movie selectMovie(final File movieFile, final String userQuery, final Collection<Movie> options, final Map<String, Object> memory, final Component parent) throws Exception {
		// 1. movie by filename
		final String fileQuery = (userQuery != null) ? userQuery : checkedStripReleaseInfo(movieFile);

		// 2. movie by directory
		final File movieFolder = guessMovieFolder(movieFile);
		final String folderQuery = (userQuery != null || movieFolder == null) ? "" : checkedStripReleaseInfo(movieFolder);

		// auto-ignore invalid files
		if (userQuery == null && fileQuery.length() < 2 && folderQuery.length() < 2) {
			return null;
		}

		if (options.size() == 1) {
			return options.iterator().next();
		}

		// auto-select perfect match
		for (Movie movie : options) {
			String movieIdentifier = normalizePunctuation(movie.toString()).toLowerCase();
			if (fileQuery.toLowerCase().startsWith(movieIdentifier) || folderQuery.toLowerCase().startsWith(movieIdentifier)) {
				return movie;
			}
		}

		// auto-select most probable search result
		final List<Movie> probableMatches = new LinkedList<Movie>();

		final SimilarityMetric metric = new NameSimilarityMetric();
		final float threshold = 0.9f;

		// find probable matches using name similarity >= 0.9
		for (Movie result : options) {
			float maxSimilarity = 0;
			for (String query : new String[] { fileQuery, folderQuery }) {
				for (String name : result.getEffectiveNamesWithoutYear()) {
					if (maxSimilarity >= threshold)
						continue;

					maxSimilarity = Math.max(maxSimilarity, metric.getSimilarity(query, name));
				}
			}
			if (maxSimilarity >= threshold) {
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

				selectDialog.setTitle(folderQuery.isEmpty() ? fileQuery : String.format("%s / %s", folderQuery, fileQuery));
				selectDialog.getHeaderLabel().setText(String.format("Movies matching '%s':", fileQuery.length() >= 2 || folderQuery.length() <= 2 ? fileQuery : folderQuery));
				selectDialog.getCancelAction().putValue(Action.NAME, "Ignore");

				// add repeat button
				JCheckBox checkBox = new JCheckBox();
				checkBox.setToolTipText("Select / Ignore for all");
				checkBox.setCursor(getPredefinedCursor(HAND_CURSOR));
				checkBox.setIcon(ResourceManager.getIcon("button.repeat"));
				checkBox.setSelectedIcon(ResourceManager.getIcon("button.repeat.selected"));
				JComponent c = (JComponent) selectDialog.getContentPane();
				c.add(checkBox, "pos 1al select.y n select.y2");

				// restore original dialog size
				Settings prefs = Settings.forPackage(MovieHashMatcher.class);
				int w = Integer.parseInt(prefs.get("dialog.select.w", "280"));
				int h = Integer.parseInt(prefs.get("dialog.select.h", "300"));
				selectDialog.setPreferredSize(new Dimension(w, h));
				selectDialog.pack();

				// show dialog
				selectDialog.setLocation(getOffsetLocation(selectDialog.getOwner()));
				selectDialog.setVisible(true);

				// remember dialog size
				prefs.put("dialog.select.w", Integer.toString(selectDialog.getWidth()));
				prefs.put("dialog.select.h", Integer.toString(selectDialog.getHeight()));

				// remember if we should auto-repeat the chosen action in the future
				if (checkBox.isSelected()) {
					memory.put("repeat", selectDialog.getSelectedValue() != null ? "select" : "ignore");
				}

				// selected value or null if the dialog was canceled by the user
				return selectDialog.getSelectedValue();
			}
		});

		// allow only one select dialog at a time
		@SuppressWarnings("unchecked")
		Map<String, Movie> selectionMemory = (Map<String, Movie>) memory.get("selection");

		if (selectionMemory.containsKey(fileQuery)) {
			return selectionMemory.get(fileQuery);
		}

		// check auto-selection settings
		if ("select".equals(memory.get("repeat"))) {
			return options.iterator().next();
		}
		if ("ignore".equals(memory.get("repeat"))) {
			return null;
		}

		// ask for user input
		SwingUtilities.invokeAndWait(showSelectDialog);

		// cache selected value
		selectionMemory.put(fileQuery, showSelectDialog.get());
		return showSelectDialog.get();
	}

	public List<Match<File, ?>> justFetchMovieInfo(final Locale locale, final Component parent) throws Exception {
		// require user input
		String input = showInputDialog("Enter movie name:", "", "Fetch Movie Info", parent);

		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		if (input != null && input.length() > 0) {
			Collection<Movie> results = new LinkedHashSet<Movie>();
			results.addAll(service.searchMovie(input, locale));
			results.addAll(matchMovieName(singleton(input), false, 2));

			// improve ranking
			results = sortBySimilarity(results, singleton(input), getMovieMatchMetric(), false);

			for (Movie it : results) {
				matches.add(new Match<File, Movie>(null, it));
			}
		}
		return matches;
	}

}
