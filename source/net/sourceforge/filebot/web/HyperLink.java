
package net.sourceforge.filebot.web;


import java.net.URI;
import java.net.URL;


public class HyperLink extends SearchResult {
	
	private final URL url;
	
	
	public HyperLink(String name, URL url) {
		super(name);
		this.url = url;
	}
	

	public URL getUrl() {
		return url;
	}
	

	public URI toUri() {
		return URI.create(url.toString());
	}
	
}
