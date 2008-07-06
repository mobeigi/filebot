
package net.sourceforge.filebot.web;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class HyperLink extends SearchResult {
	
	private final URL url;
	
	
	public HyperLink(String name, URL url) {
		super(name);
		this.url = url;
	}
	

	public URL getURL() {
		return url;
	}
	

	public URI toURI() {
		try {
			return url.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
}
