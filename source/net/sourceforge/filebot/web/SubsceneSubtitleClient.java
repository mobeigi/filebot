
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getHtmlDocument;
import static net.sourceforge.tuned.XPathUtilities.getAttribute;
import static net.sourceforge.tuned.XPathUtilities.getChild;
import static net.sourceforge.tuned.XPathUtilities.getChildren;
import static net.sourceforge.tuned.XPathUtilities.getTextContent;
import static net.sourceforge.tuned.XPathUtilities.selectNodes;
import static net.sourceforge.tuned.XPathUtilities.selectString;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.FileUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class SubsceneSubtitleClient implements SubtitleClient {
	
	private static final String host = "subscene.com";
	
	private final Map<String, Integer> languageFilterMap = new HashMap<String, Integer>(50);
	
	
	@Override
	public String getName() {
		return "Subscene";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.subscene");
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		
		URL searchUrl = new URL("http", host, "/filmsearch.aspx?q=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("id('filmSearch')/A", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String title = getTextContent(node);
			String href = getAttribute("href", node);
			
			try {
				searchResults.add(new HyperLink(title, new URL("http", host, href)));
			} catch (MalformedURLException e) {
				Logger.getLogger("global").log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		// we might have been redirected to the subtitle list
		if (searchResults.isEmpty()) {
			try {
				// get name of current search result
				String name = selectString("id('leftWrapperWide')//H1/text()", dom);
				
				// get current location
				String file = selectString("id('aspnetForm')/@action", dom);
				
				searchResults.add(new HyperLink(name, new URL("http", host, file)));
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.WARNING, "Cannot parse subtitle page: " + searchUrl, e);
			}
		}
		
		return searchResults;
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception {
		URL subtitleListUrl = getSubtitleListLink(searchResult, language).toURL();
		
		// english language name or null
		String languageName = (language == null || language.equals(Locale.ROOT) ? null : language.getDisplayLanguage(Locale.ENGLISH));
		Integer languageFilter = null;
		
		if (languageName != null) {
			synchronized (languageFilterMap) {
				languageFilter = languageFilterMap.get(languageName.toLowerCase());
			}
		}
		
		Document subtitleListDocument = getSubtitleListDocument(subtitleListUrl, languageFilter);
		
		// let's update language filters if they are not known yet
		if (languageFilterMap.isEmpty()) {
			synchronized (languageFilterMap) {
				languageFilterMap.putAll(getLanguageFilterMap(subtitleListDocument));
			}
		}
		
		return getSubtitleList(subtitleListUrl, languageName, subtitleListDocument);
	}
	

	private List<SubtitleDescriptor> getSubtitleList(URL subtitleListUrl, String languageName, Document subtitleListDocument) {
		
		List<Node> nodes = selectNodes("//TABLE[@class='filmSubtitleList']//A[@class='a1']", subtitleListDocument);
		
		// match subtitleId and typeId 
		Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', .*");
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>(nodes.size());
		
		for (Node node : nodes) {
			try {
				String lang = getTextContent(getChildren("SPAN", node).get(0));
				
				if (languageName == null || languageName.equalsIgnoreCase(lang)) {
					String name = getTextContent(getChildren("SPAN", node).get(1));
					String href = getAttribute("href", node);
					
					Matcher matcher = hrefPattern.matcher(href);
					
					if (!matcher.matches())
						throw new IllegalArgumentException("Cannot parse download parameters: " + href);
					
					String subtitleId = matcher.group(1);
					String typeId = matcher.group(2);
					
					URL downloadUrl = getDownloadUrl(subtitleListUrl, subtitleId, typeId);
					
					subtitles.add(new SubsceneSubtitleDescriptor(name, lang, typeId, downloadUrl, subtitleListUrl));
				}
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return subtitles;
	}
	

	protected Document getSubtitleListDocument(URL subtitleListUrl, Integer languageFilter) throws IOException, SAXException {
		URLConnection connection = subtitleListUrl.openConnection();
		
		if (languageFilter != null) {
			connection.addRequestProperty("Cookie", "subscene_sLanguageIds=" + languageFilter);
		}
		
		return getHtmlDocument(connection);
	}
	

	protected Map<String, Integer> getLanguageFilterMap(Document subtitleListDocument) {
		Map<String, Integer> filters = new HashMap<String, Integer>(50);
		
		List<Node> nodes = selectNodes("//DIV[@class='languageList']/DIV", subtitleListDocument);
		
		for (Node node : nodes) {
			// select INPUT/@onclick, ditch non-number-characters
			String filter = getAttribute("onclick", getChild("INPUT", node)).replaceAll("\\D+", "");
			
			if (filter != null) {
				// select LABEL/text()
				String name = getTextContent("LABEL", node);
				
				filters.put(name.toLowerCase(), Integer.valueOf(filter));
			}
		}
		
		return filters;
	}
	

	protected URL getDownloadUrl(URL referer, String subtitleId, String typeId) throws MalformedURLException {
		String basePath = FileUtilities.getNameWithoutExtension(referer.getFile());
		String path = String.format("%s-dlpath-%s/%s.zipx", basePath, subtitleId, typeId);
		
		return new URL(referer.getProtocol(), referer.getHost(), path);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, Locale locale) {
		return ((HyperLink) searchResult).toURI();
	}
	
}
