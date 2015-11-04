package net.filebot.web;

public class TVMazeSearchResult extends SearchResult {

	protected int id;

	protected TVMazeSearchResult() {
		// used by serializer
	}

	public TVMazeSearchResult(int id, String name) {
		super(name, new String[0]);
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof TVMazeSearchResult) {
			TVMazeSearchResult other = (TVMazeSearchResult) object;
			return this.id == other.id;
		}

		return false;
	}

	@Override
	public TVMazeSearchResult clone() {
		return new TVMazeSearchResult(id, name);
	}

}
