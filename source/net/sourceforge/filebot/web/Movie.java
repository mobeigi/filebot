
package net.sourceforge.filebot.web;


import java.util.Arrays;


public class Movie extends SearchResult {
	
	protected int year;
	protected int imdbId;
	

	protected Movie() {
		// used by serializer
	}
	

	public Movie(String name, int year, int imdbId) {
		super(name);
		this.year = year;
		this.imdbId = imdbId;
	}
	

	public int getYear() {
		return year;
	}
	

	public int getImdbId() {
		return imdbId;
	}
	

	@Override
	public boolean equals(Object object) {
		if (object instanceof Movie) {
			Movie other = (Movie) object;
			return imdbId == other.imdbId && year == other.year && name.equals(other.name);
		}
		
		return false;
	}
	

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { name, year, imdbId });
	}
	

	@Override
	public String toString() {
		return String.format("%s (%d)", name, year);
	}
	
}
