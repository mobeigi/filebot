
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private String title;
	private String language;
	
	private URL subtitlePage;
	private Map<String, String> subtitleInfo;
	
	
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
		URL downloadLink = new URL(subtitlePage.getProtocol(), subtitlePage.getHost(), "/subtitle/download");
		
		HttpURLConnection connection = (HttpURLConnection) downloadLink.openConnection();
		connection.addRequestProperty("Referer", subtitlePage.toString());
		
		return WebRequest.post(connection, getSubtitleInfo());
	}
	
	
	private synchronized Map<String, String> getSubtitleInfo() {
		// extract subtitle information from subtitle page if necessary
		if (subtitleInfo == null) {
			subtitleInfo = new HashMap<String, String>();
			try {
				Document dom = getHtmlDocument(subtitlePage);
				for (Node input : selectNodes("id('dl')//INPUT[@name]", dom)) {
					subtitleInfo.put(getAttribute("name", input), getAttribute("value", input));
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Failed to extract subtitle info", e);
			}
		}
		return subtitleInfo;
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
