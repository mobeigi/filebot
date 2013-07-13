
package net.sourceforge.filebot.web;


public class SerienjunkiesSearchResult extends SearchResult {
	
	protected int sid;
	protected String link;
	protected String mainTitle;
	protected String germanTitle;
	protected Date startDate;
	
	
	protected SerienjunkiesSearchResult() {
		// used by serializer
	}
	
	
	public SerienjunkiesSearchResult(int sid, String link, String mainTitle, String germanTitle, Date startDate) {
		this.sid = sid;
		this.link = link;
		this.mainTitle = mainTitle;
		this.germanTitle = germanTitle;
		this.startDate = startDate;
	}
	
	
	public int getId() {
		return sid;
	}
	
	
	@Override
	public String getName() {
		return germanTitle != null ? germanTitle : mainTitle; // prefer German title
	}
	
	
	public int getSeriesId() {
		return sid;
	}
	
	
	public String getLink() {
		return link;
	}
	
	
	public String getMainTitle() {
		return mainTitle;
	}
	
	
	public String getGermanTitle() {
		return germanTitle;
	}
	
	
	public Date getStartDate() {
		return startDate;
	}
	
	
	@Override
	public int hashCode() {
		return sid;
	}
	
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof SerienjunkiesSearchResult) {
			SerienjunkiesSearchResult other = (SerienjunkiesSearchResult) object;
			return this.sid == other.sid;
		}
		
		return false;
	}
}