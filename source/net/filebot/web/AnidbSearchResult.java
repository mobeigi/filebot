package net.filebot.web;

public class AnidbSearchResult extends SearchResult {

	protected AnidbSearchResult() {
		// used by serializer
	}

	public AnidbSearchResult(int aid, String primaryTitle, String[] aliasNames) {
		super(aid, primaryTitle, aliasNames);
	}

	public int getAnimeId() {
		return id;
	}

	public String getPrimaryTitle() {
		return name;
	}

	@Override
	public AnidbSearchResult clone() {
		return new AnidbSearchResult(id, name, aliasNames);
	}

}
