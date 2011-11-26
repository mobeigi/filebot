
package net.sourceforge.filebot.web;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;


public class HyperLink extends SearchResult {
	
	protected URL url;
	

	protected HyperLink() {
		// used by serializer
	}
	

	public HyperLink(String name, URL url) {
		super(name);
		this.url = url;
	}
	

	public URL getURL() {
		return url;
	}
	

	public URI getURI() {
		try {
			return url.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public boolean equals(Object object) {
		if (object instanceof HyperLink) {
			HyperLink other = (HyperLink) object;
			return name.equals(name) && url.toString().equals(other.url.toString());
		}
		
		return false;
	}
	

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { name, url.toString() });
	}
	
}
