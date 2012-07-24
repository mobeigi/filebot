
package net.sourceforge.filebot.web;


public class MoviePart extends Movie {
	
	protected final int partIndex;
	protected final int partCount;
	
	
	public MoviePart(MoviePart obj) {
		this(obj.name, obj.year, obj.imdbId, obj.tmdbId, obj.partIndex, obj.partCount);
	}
	
	
	public MoviePart(Movie movie, int partIndex, int partCount) {
		this(movie.name, movie.year, movie.imdbId, movie.tmdbId, partIndex, partCount);
	}
	
	
	public MoviePart(String name, int year, int imdbId, int tmdbId, int partIndex, int partCount) {
		super(name, year, imdbId, tmdbId);
		this.partIndex = partIndex;
		this.partCount = partCount;
	}
	
	
	public int getPartIndex() {
		return partIndex;
	}
	
	
	public int getPartCount() {
		return partCount;
	}
	
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof MoviePart && super.equals(object)) {
			MoviePart other = (MoviePart) object;
			return partIndex == other.partIndex && partCount == other.partCount;
		}
		
		return super.equals(object);
	}
	
	
	@Override
	public MoviePart clone() {
		return new MoviePart(this);
	}
	
	
	@Override
	public String toString() {
		return String.format("%s (%d) [%d]", name, year, partIndex);
	}
	
}
