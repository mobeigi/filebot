package net.filebot.web;

public class TheTVDBSearchResult extends SearchResult {

	protected TheTVDBSearchResult() {
		// used by serializer
	}

	public TheTVDBSearchResult(String seriesName, int seriesId) {
		super(seriesId, seriesName);
	}

	public TheTVDBSearchResult(String seriesName, String[] aliasNames, int seriesId) {
		super(seriesId, seriesName, aliasNames);
	}

	public int getSeriesId() {
		return id;
	}

	@Override
	public TheTVDBSearchResult clone() {
		return new TheTVDBSearchResult(name, aliasNames, id);
	}

}
