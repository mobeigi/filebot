
package net.sourceforge.filebot.web;


import java.io.Serializable;


public abstract class SearchResult implements Serializable {
	
	private final String name;
	
	
	public SearchResult(String name) {
		this.name = name;
	}
	

	public String getName() {
		return name;
	}
	

	@Override
	public String toString() {
		return name;
	}
	
}
