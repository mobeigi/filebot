
package net.sourceforge.filebot.web;


import java.util.concurrent.ConcurrentHashMap;


public class SearchResultCache {
	
	private final ConcurrentHashMap<String, SearchResult> cache = new ConcurrentHashMap<String, SearchResult>();
	
	
	public boolean containsKey(String name) {
		return cache.containsKey(key(name));
	}
	

	public SearchResult get(String name) {
		return cache.get(key(name));
	}
	

	public void add(SearchResult searchResult) {
		cache.putIfAbsent(key(searchResult.getName()), searchResult);
	}
	

	public void addAll(Iterable<SearchResult> searchResults) {
		for (SearchResult searchResult : searchResults) {
			add(searchResult);
		}
	}
	

	private String key(String name) {
		return name.toLowerCase();
	}
	
}
