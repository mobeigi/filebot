
package net.sourceforge.filebot.web;


import java.io.File;
import java.util.List;


public interface MovieIdentificationService {
	
	public List<MovieDescriptor> searchMovie(String query) throws Exception;
	

	public MovieDescriptor getMovieDescriptor(int imdbid) throws Exception;
	

	public MovieDescriptor[] getMovieDescriptors(File[] movieFiles) throws Exception;
	
}
