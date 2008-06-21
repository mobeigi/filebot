
package net.sourceforge.filebot.web;


import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


public class SearchResultCache {
	
	private final Map<String, SearchResult> cache = Collections.synchronizedMap(new TreeMap<String, SearchResult>(String.CASE_INSENSITIVE_ORDER));
	
	
	public boolean containsKey(String name) {
		return cache.containsKey(name);
	}
	

	public SearchResult get(String name) {
		return cache.get(name);
	}
	

	public void add(SearchResult searchResult) {
		cache.put(searchResult.getName(), searchResult);
	}
	

	public void addAll(Iterable<SearchResult> searchResults) {
		for (SearchResult searchResult : searchResults) {
			add(searchResult);
		}
	}
}
