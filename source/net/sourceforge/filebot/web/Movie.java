
package net.sourceforge.filebot.web;




public class Movie {
	
	private String title;
	private Integer year;
	private Integer imdbID;
	
	
	public Movie(String title, Integer year, Integer imdbID) {
		this.title = title;
		this.imdbID = imdbID;
		this.year = year;
	}
	

	public String getTitle() {
		return title;
	}
	

	public Integer getImdbID() {
		return imdbID;
	}
	

	public Integer getYear() {
		return year;
	}
	

	@Override
	public String toString() {
		if (year == null)
			return title;
		
		return String.format("%s (%d)", title, year);
	}
}
