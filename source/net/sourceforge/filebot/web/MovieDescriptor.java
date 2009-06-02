
package net.sourceforge.filebot.web;


public class MovieDescriptor extends SearchResult {
	
	private final int year;
	private final int imdbId;
	

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
			return getImdbId() == other.getImdbId() && getName().equals(other.getName()) && getYear() == other.getYear();
		}
		
		return false;
	}
	

	@Override
	public String toString() {
		return String.format("%s (%d)", getName(), getYear());
	}
	
}
