
package net.sourceforge.filebot.web;


public class MovieDescriptor {
	
	private final String title;
	private final Integer imdbId;
	
	
	public MovieDescriptor(String title) {
		this(title, null);
	}
	

	public MovieDescriptor(String title, Integer imdbId) {
		this.title = title;
		this.imdbId = imdbId;
	}
	

	public String getTitle() {
		return title;
	}
	

	public Integer getImdbId() {
		return imdbId;
	}
	

	@Override
	public String toString() {
		return title;
	}
	
}
