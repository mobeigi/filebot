package net.filebot.web;

import static java.util.Collections.*;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.List;

public abstract class SearchResult implements Serializable {

	protected String name;
	protected String[] aliasNames;

	protected SearchResult() {
		// used by serializer
	}

	public SearchResult(String name, String[] aliasNames) {
		this.name = name;
		this.aliasNames = aliasNames.clone();
	}

	public String getName() {
		return name;
	}

	public String[] getAliasNames() {
		return aliasNames.clone();
	}

	public List<String> getEffectiveNames() {
		if (name == null || name.length() == 0) {
			return emptyList();
		}
		if (aliasNames == null || aliasNames.length == 0) {
			return singletonList(name);
		}
		return new AbstractList<String>() {

			@Override
			public String get(int index) {
				return index == 0 ? name : aliasNames[index - 1];
			}

			@Override
			public int size() {
				return 1 + aliasNames.length;
			}
		};
	}

	@Override
	public abstract SearchResult clone();

	@Override
	public String toString() {
		return name;
	}

}
