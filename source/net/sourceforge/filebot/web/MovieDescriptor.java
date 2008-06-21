
package net.sourceforge.filebot.web;


public class MovieDescriptor extends SearchResult {
	
	private final int imdbId;
	
	
	public MovieDescriptor(String name, int imdbId) {
		super(name);
		this.imdbId = imdbId;
	}
	

	public int getImdbId() {
		return imdbId;
	}
	
}
