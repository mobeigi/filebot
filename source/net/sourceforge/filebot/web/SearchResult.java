package net.sourceforge.filebot.web;

import java.io.Serializable;

public abstract class SearchResult implements Serializable {

	protected String name;
	protected String[] aliasNames;

	protected SearchResult() {
		// used by serializer
	}

	public SearchResult(String name, String... aliasNames) {
		this.name = name;
		this.aliasNames = aliasNames;
	}

	public String getName() {
		return name;
	}

	public String[] getAliasNames() {
		return aliasNames.clone();
	}

	@Override
	public String toString() {
		return name;
	}

}
