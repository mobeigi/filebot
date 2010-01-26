
package net.sourceforge.filebot.ui.panel.rename;


import net.sourceforge.filebot.web.MovieDescriptor;


class MoviePart {
	
	private final MovieDescriptor movie;
	
	private final int partIndex;
	private final int partCount;
	

	public MoviePart(MovieDescriptor movie) {
		this(movie, 0, 1);
	}
	

	public MoviePart(MovieDescriptor movie, int partIndex, int partCount) {
		if (partCount < 1 || partIndex >= partCount)
			throw new IllegalArgumentException("Illegal part: " + partIndex + "/" + partCount);
		
		this.movie = movie;
		this.partIndex = partIndex;
		this.partCount = partCount;
	}
	

	public MovieDescriptor getMovie() {
		return movie;
	}
	

	public int getPartIndex() {
		return partIndex;
	}
	

	public int getPartCount() {
		return partCount;
	}
	
}
