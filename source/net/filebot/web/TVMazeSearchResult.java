package net.filebot.web;

public class TVMazeSearchResult extends SearchResult {

	protected TVMazeSearchResult() {
		// used by serializer
	}

	public TVMazeSearchResult(int id, String name) {
		super(id, name);
	}

	@Override
	public TVMazeSearchResult clone() {
		return new TVMazeSearchResult(id, name);
	}

}
