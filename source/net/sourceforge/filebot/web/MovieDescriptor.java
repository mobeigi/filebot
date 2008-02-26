
package net.sourceforge.filebot.web;


import java.net.URL;


public class MovieDescriptor {
	
	private String title;
	private Integer imdbId;
	
	private Integer year;
	private URL imdbUrl;
	
	
	public MovieDescriptor(String description, Integer imdbId) {
		this(description, imdbId, null, null);
	}
	

	public MovieDescriptor(String title, Integer imdbId, Integer year, URL imdbUrl) {
		this.title = title;
		this.imdbId = imdbId;
		this.year = year;
		this.imdbUrl = imdbUrl;
	}
	

	public String getTitle() {
		return title;
	}
	

	public Integer getImdbId() {
		return imdbId;
	}
	

	public Integer getYear() {
		return year;
	}
	

	public URL getImdbUrl() {
		return imdbUrl;
	}
	

	@Override
	public String toString() {
		if (year == null)
			return title;
		
		return String.format("%s (%d)", title, year);
	}
}
