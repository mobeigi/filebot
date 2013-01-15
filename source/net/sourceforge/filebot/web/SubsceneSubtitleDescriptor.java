
package net.sourceforge.filebot.web;


import static java.util.Collections.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private String title;
	private String language;
	
	private URL subtitlePage;
	
	
	public SubsceneSubtitleDescriptor(String title, String language, URL subtitlePage) {
		this.title = title;
		this.language = language;
		this.subtitlePage = subtitlePage;
	}
	
	
	@Override
	public String getName() {
		return title;
	}
	
	
	@Override
	public String getLanguageName() {
		return language;
	}
	
	
	@Override
	public String getType() {
		return null;
	}
	
	
	@Override
	public ByteBuffer fetch() throws Exception {
		return WebRequest.fetch(getDownloadLink(), 0, singletonMap("Referer", subtitlePage.toString()));
	}
	
	
	private URL getDownloadLink() throws IOException, SAXException {
		Document page = WebRequest.getHtmlDocument(subtitlePage);
		String file = selectString("id('downloadButton')/@href", page);
		return new URL(subtitlePage.getProtocol(), subtitlePage.getHost(), file);
	}
	
	
	@Override
	public String getPath() {
		return getName();
	}
	
	
	@Override
	public long getLength() {
		return -1;
	}
	
	
	@Override
	public int hashCode() {
		return subtitlePage.getPath().hashCode();
	}
	
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof SubsceneSubtitleDescriptor) {
			SubsceneSubtitleDescriptor other = (SubsceneSubtitleDescriptor) object;
			return subtitlePage.getPath().equals(other.getPath());
		}
		
		return false;
	}
	
	
	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
