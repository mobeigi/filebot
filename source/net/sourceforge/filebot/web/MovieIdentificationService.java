
package net.sourceforge.filebot.web;


import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;


public interface MovieIdentificationService {
	
	public String getName();
	

	public Icon getIcon();
	

	public List<MovieDescriptor> searchMovie(String query, Locale locale) throws Exception;
	

	public MovieDescriptor getMovieDescriptor(int imdbid, Locale locale) throws Exception;
	

	public MovieDescriptor[] getMovieDescriptors(File[] movieFiles, Locale locale) throws Exception;
	
}
