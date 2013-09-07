package net.sourceforge.filebot.web;

public class SerienjunkiesSearchResult extends SearchResult {

	protected int sid;
	protected String link;
	protected Date startDate;

	protected SerienjunkiesSearchResult() {
		// used by serializer
	}

	public SerienjunkiesSearchResult(int sid, String link, String germanTitle, String[] otherTitles, Date startDate) {
		super(germanTitle, otherTitles);
		this.sid = sid;
		this.link = link;
		this.startDate = startDate;
	}

	public int getId() {
		return sid;
	}

	public int getSeriesId() {
		return sid;
	}

	public String getLink() {
		return link;
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