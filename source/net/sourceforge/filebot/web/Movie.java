
package net.sourceforge.filebot.web;


import java.util.Arrays;


public class Movie extends SearchResult {
	
	protected int year;
	protected int imdbId;
	protected int tmdbId;
	
	
	protected Movie() {
		// used by serializer
	}
	
	
	public Movie(Movie obj) {
		this(obj.name, obj.year, obj.imdbId, obj.tmdbId);
	}
	
	
	public Movie(String name, int year, int imdbId, int tmdbId) {
		super(name);
		this.year = year;
		this.imdbId = imdbId;
		this.tmdbId = tmdbId;
	}
	
	
	public int getYear() {
		return year;
	}
	
	
	public int getImdbId() {
		return imdbId;
	}
	
	
	public int getTmdbId() {
		return tmdbId;
	}
	
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Movie) {
			Movie other = (Movie) object;
			if (imdbId > 0 && other.imdbId > 0) {
				return imdbId == other.imdbId;
			} else if (tmdbId > 0 && other.tmdbId > 0) {
				return tmdbId == other.tmdbId;
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
		return Arrays.hashCode(new Object[] { name, year });
	}
	
	
	@Override
	public String toString() {
		return String.format("%s (%04d)", name, year < 0 ? 0 : year);
	}
	
}
