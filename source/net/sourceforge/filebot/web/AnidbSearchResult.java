
package net.sourceforge.filebot.web;


public class AnidbSearchResult extends SearchResult {
	
	protected int aid;
	protected String primaryTitle; // one per anime
	protected String englishTitle; // one per language
	
	
	protected AnidbSearchResult() {
		// used by serializer
	}
	
	
	public AnidbSearchResult(int aid, String primaryTitle, String englishTitle) {
		this.aid = aid;
		this.primaryTitle = primaryTitle;
		this.englishTitle = englishTitle;
	}
	
	
	public int getId() {
		return aid;
	}
	
	
	public int getAnimeId() {
		return aid;
	}
	
	
	@Override
	public String getName() {
		return primaryTitle;
	}
	
	
	public String getPrimaryTitle() {
		return primaryTitle;
	}
	
	
	public String getEnglishTitle() {
		return englishTitle;
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