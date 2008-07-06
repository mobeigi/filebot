
package net.sourceforge.filebot.web;


import java.net.URI;
import java.net.URISyntaxException;


public class HyperLink extends SearchResult {
	
	private final URI uri;
	
	
	public HyperLink(String name, URI uri) {
		super(name);
		this.uri = uri;
	}
	

	public HyperLink(String name, String uri) throws URISyntaxException {
		this(name, new URI(uri));
	}
	

	public URI getURI() {
		return uri;
	}
	
}
