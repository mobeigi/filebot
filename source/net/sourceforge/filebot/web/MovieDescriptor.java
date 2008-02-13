
package net.sourceforge.filebot.web;


import java.net.URL;


public class MovieDescriptor {
	
	private String title;
	private int year;
	private int imdbId;
	private URL imdbUrl;
	
	
	public MovieDescriptor(String title, int year, int imdbId, URL imdbUrl) {
		this.title = title;
		this.imdbId = imdbId;
		this.year = year;
		this.imdbUrl = imdbUrl;
	}
	

	public String getTitle() {
		return title;
	}
	

	public int getYear() {
		return year;
	}
	

	public int getImdbId() {
		return imdbId;
	}
	

	public URL getImdbUrl() {
		return imdbUrl;
	}
	

	@Override
	public String toString() {
		return String.format("%s (%d)", title, year);
	}
}
