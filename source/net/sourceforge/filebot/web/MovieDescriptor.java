
package net.sourceforge.filebot.web;


import java.util.Arrays;


public class MovieDescriptor extends SearchResult {
	
	private final int year;
	private final int imdbId;
	

	public MovieDescriptor(String name, int imdbId) {
		this(name, -1, imdbId);
	}
	

	public MovieDescriptor(String name, int year, int imdbId) {
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
		if (object instanceof MovieDescriptor) {
			MovieDescriptor other = (MovieDescriptor) object;
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
		if (year < 0)
			return name;
		
		return String.format("%s (%d)", name, year);
	}
	
}
