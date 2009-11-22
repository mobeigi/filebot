
package net.sourceforge.filebot.web;


import java.io.File;


public interface MovieIdentificationService {
	
	public MovieDescriptor getMovieDescriptor(int imdbid) throws Exception;
	

	public MovieDescriptor[] getMovieDescriptors(File[] movieFiles) throws Exception;
	
}
