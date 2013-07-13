
package net.sourceforge.filebot.web;


public class TVRageSearchResult extends SearchResult {
	
	protected int showId;
	protected String link;
	
	
	protected TVRageSearchResult() {
		// used by serializer
	}
	
	
	public TVRageSearchResult(String name, int showId, String link) {
		super(name);
		this.showId = showId;
		this.link = link;
	}
	
	
	public int getId() {
		return showId;
	}
	
	
	public int getSeriesId() {
		return showId;
	}
	
	
	public String getLink() {
		return link;
	}
	
	
	@Override
	public int hashCode() {
		return showId;
	}
	
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof TVRageSearchResult) {
			TVRageSearchResult other = (TVRageSearchResult) object;
			return this.showId == other.showId;
		}
		
		return false;
	}
}