
package net.sourceforge.filebot.web;


public class MovieDescriptor extends SearchResult {
	
	private final int imdbId;
	
	
	public MovieDescriptor(String name, int imdbId) {
		super(name);
		this.imdbId = imdbId;
	}
	

	public int getImdbId() {
		return imdbId;
	}
	

	@Override
	public boolean equals(Object object) {
		if (object instanceof MovieDescriptor) {
			MovieDescriptor other = (MovieDescriptor) object;
			return this.getImdbId() == other.getImdbId() && this.getName() == other.getName();
		}
		
		return super.equals(object);
	}
	
}
