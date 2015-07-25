package net.filebot.web;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

public interface MovieIdentificationService extends Datasource {

	public String getName();

	public Icon getIcon();

	public List<Movie> searchMovie(String query, Locale locale) throws Exception;

	public Movie getMovieDescriptor(Movie movie, Locale locale) throws Exception;

	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception;

}
