package net.sourceforge.filebot.web;


public class TheTVDBSearchResult extends SearchResult {

	protected int seriesId;

	protected TheTVDBSearchResult() {
		// used by serializer
	}

	public TheTVDBSearchResult(String seriesName, int seriesId) {
		this(seriesName, new String[0], seriesId);
	}

	public TheTVDBSearchResult(String seriesName, String[] aliasNames, int seriesId) {
		super(seriesName, aliasNames);
		this.seriesId = seriesId;
	}

	public int getId() {
		return seriesId;
	}

	public int getSeriesId() {
		return seriesId;
	}

	@Override
	public int hashCode() {
		return seriesId;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof TheTVDBSearchResult) {
			TheTVDBSearchResult other = (TheTVDBSearchResult) object;
			return this.seriesId == other.seriesId;
		}

		return false;
	}
}