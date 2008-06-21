
package net.sourceforge.filebot.web;


import java.net.URI;
import java.net.URL;


public class HyperLink extends SearchResult {
	
	private final URI uri;
	
	
	public HyperLink(String name, URI uri) {
		super(name);
		this.uri = uri;
	}
	

	public HyperLink(String name, URL url) {
		super(name);
		this.uri = URI.create(url.toString());
	}
	

	public URI getUri() {
		return uri;
	}
	
}
