package net.sourceforge.filebot.web;

public class AnidbSearchResult extends SearchResult {

	protected int aid;

	protected AnidbSearchResult() {
		// used by serializer
	}

	public AnidbSearchResult(int aid, String primaryTitle, String[] aliasNames) {
		super(primaryTitle, aliasNames);
		this.aid = aid;
	}

	public int getId() {
		return aid;
	}

	public int getAnimeId() {
		return aid;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getPrimaryTitle() {
		return name;
	}

	@Override
	public int hashCode() {
		return aid;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof AnidbSearchResult) {
			AnidbSearchResult other = (AnidbSearchResult) object;
			return this.aid == other.aid;
		}

		return false;
	}
}