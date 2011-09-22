
package net.sourceforge.filebot.web;


public class MoviePart extends Movie {
	
	protected final int partIndex;
	protected final int partCount;
	

	public MoviePart(Movie movie, int partIndex, int partCount) {
		super(movie.name, movie.year, movie.imdbId);
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
	public String toString() {
		return String.format("%s (%d) [%d]", name, year, partIndex);
	}
	
}
