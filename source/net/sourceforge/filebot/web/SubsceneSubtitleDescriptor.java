
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import net.sourceforge.tuned.FileUtilities;


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
		return getSubtitleInfo().get("typeId");
	}
	

	@Override
	public ByteBuffer fetch() throws Exception {
		// e.g. http://subscene.com/english/Firefly-The-Complete-Series/subtitle-40003-dlpath-20008/rar.zipx
		String subtitlePagePath = FileUtilities.getNameWithoutExtension(subtitlePage.getFile());
		String path = String.format("%s-dlpath-%s/%s.zipx", subtitlePagePath, getSubtitleInfo().get("filmId"), getSubtitleInfo().get("typeId"));
		
		URL downloadLocator = new URL(subtitlePage.getProtocol(), subtitlePage.getHost(), path);
		Map<String, String> downloadPostData = subtitleInfo;
		
		HttpURLConnection connection = (HttpURLConnection) downloadLocator.openConnection();
		connection.addRequestProperty("Referer", subtitlePage.toString());
		
		return WebRequest.post(connection, downloadPostData);
	}
	

	private synchronized Map<String, String> getSubtitleInfo() {
		// extract subtitle information from subtitle page if necessary
		if (subtitleInfo == null) {
			try {
				Document dom = getHtmlDocument(subtitlePage);
				
				subtitleInfo = new HashMap<String, String>();
				subtitleInfo.put("subtitleId", selectString("//INPUT[@name='subtitleId']/@value", dom));
				subtitleInfo.put("typeId", selectString("//INPUT[@name='typeId']/@value", dom));
				subtitleInfo.put("filmId", selectString("//INPUT[@name='filmId']/@value", dom));
			} catch (Exception e) {
				throw new RuntimeException("Failed to extract subtitle info", e);
			}
		}
		
		return subtitleInfo;
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
