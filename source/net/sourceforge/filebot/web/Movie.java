
package net.sourceforge.filebot.web;


public class Movie extends SearchResult {
	
	protected int year;
	protected int imdbId;
	
	
	protected Movie() {
		// used by serializer
	}
	
	
	public Movie(Movie obj) {
		this(obj.name, obj.year, obj.imdbId);
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
			if (imdbId > 0 && other.imdbId > 0) {
				return imdbId == other.imdbId;
			}
			
			return year == other.year && name.equals(other.name);
		}
		
		return false;
	}
	
	
	@Override
	public Movie clone() {
		return new Movie(this);
	}
	
	
	@Override
	public int hashCode() {
		return imdbId;
	}
	
	
	@Override
	public String toString() {
		return String.format("%s (%04d)", name, year < 0 ? 0 : year);
	}
	
}
