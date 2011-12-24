
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;


public class SubsceneSubtitleClient implements SubtitleProvider {
	
	private static final String host = "subscene.com";
	
	
	@Override
	public String getName() {
		return "Subscene";
	}
	
	
	@Override
	public URI getLink() {
		return URI.create("http://subscene.com");
	}
	
	
	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.subscene");
	}
	
	
	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		URL searchUrl = new URL("http", host, "/filmsearch.aspx?q=" + encode(query));
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("id('filmSearch')/A", dom);
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		Pattern titleSuffixPattern = Pattern.compile("\\s-\\s([^-]+)[(](\\d{4})[)]$");
		
		for (Node node : nodes) {
			String title = getTextContent(node);
			String href = getAttribute("href", node);
			
			// simplified name for easy matching
			String shortName = titleSuffixPattern.matcher(title).replaceFirst("");
			
			try {
				searchResults.add(new SubsceneSearchResult(shortName, title, new URL("http", host, href)));
			} catch (MalformedURLException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		// we might have been redirected to the subtitle list
		if (searchResults.isEmpty()) {
			try {
				// get name of current search result
				String name = selectString("id('leftWrapperWide')//H1/text()", dom);
				
				// get current location
				String file = selectString("id('aspnetForm')/@action", dom);
				
				if (!name.isEmpty() && !file.isEmpty()) {
					searchResults.add(new HyperLink(name, new URL("http", host, file)));
				}
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot parse subtitle page: " + searchUrl, e);
			}
		}
		
		return searchResults;
	}
	
	
	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		URL subtitleListUrl = getSubtitleListLink(searchResult, languageName).toURL();
		
		String languageFilter = getLanguageFilter(languageName);
		Document subtitleListDocument = getSubtitleListDocument(subtitleListUrl, languageFilter);
		
		// let's update language filters if they are not known yet
		if (languageName != null && languageFilter == null) {
			updateLanguageFilterMap(subtitleListDocument);
		}
		
		return getSubtitleList(subtitleListUrl, languageName, subtitleListDocument);
	}
	
	
	private List<SubtitleDescriptor> getSubtitleList(URL subtitleListUrl, String languageName, Document subtitleListDocument) {
		List<Node> nodes = selectNodes("//TABLE[@class='filmSubtitleList']//A[@class='a1']", subtitleListDocument);
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>(nodes.size());
		
		for (Node node : nodes) {
			try {
				String lang = getTextContent(getChildren("SPAN", node).get(0));
				
				if (languageName == null || languageName.equalsIgnoreCase(lang)) {
					String name = getTextContent(getChildren("SPAN", node).get(1));
					String href = getAttribute("href", node);
					URL subtitlePage = new URL(subtitleListUrl.getProtocol(), subtitleListUrl.getHost(), href);
					
					subtitles.add(new SubsceneSubtitleDescriptor(name, lang, subtitlePage));
				}
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return subtitles;
	}
	
	
	protected Document getSubtitleListDocument(URL subtitleListUrl, String languageFilter) throws IOException, SAXException {
		URLConnection connection = subtitleListUrl.openConnection();
		
		if (languageFilter != null) {
			connection.addRequestProperty("Cookie", "subscene_sLanguageIds=" + languageFilter);
		}
		
		return getHtmlDocument(connection);
	}
	
	
	protected String getLanguageFilter(String languageName) {
		if (languageName == null || languageName.isEmpty()) {
			return null;
		}
		
		Cache cache = CacheManager.getInstance().getCache("web-persistent-datasource");
		String cacheKey = getClass().getName() + ".languageFilter";
		
		Element element = cache.get(cacheKey);
		if (element == null) {
			return null;
		}
		
		return (String) ((Map<?, ?>) element.getValue()).get(languageName.toLowerCase());
	}
	
	
	protected Map<String, String> updateLanguageFilterMap(Document subtitleListDocument) {
		Map<String, String> filters = new HashMap<String, String>(50);
		List<Node> nodes = selectNodes("//DIV[@class='languageList']/DIV", subtitleListDocument);
		
		for (Node node : nodes) {
			// select INPUT/@onclick, then ditch non-number-characters
			String filter = getAttribute("onclick", getChild("INPUT", node)).replaceAll("\\D+", "");
			
			if (filter != null) {
				// select LABEL/text()
				String name = getTextContent("LABEL", node);
				
				filters.put(name.toLowerCase(), filter);
			}
		}
		
		// update cache after sanity check
		if (filters.size() > 42) {
			Cache cache = CacheManager.getInstance().getCache("web-persistent-datasource");
			String cacheKey = getClass().getName() + ".languageFilter";
			cache.put(new Element(cacheKey, filters));
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to scrape language filters: " + filters);
		}
		
		return filters;
	}
	
	
	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		return ((HyperLink) searchResult).getURI();
	}
	
	
	public static class SubsceneSearchResult extends HyperLink {
		
		private String shortName;
		
		
		public SubsceneSearchResult(String shortName, String title, URL url) {
			super(title, url);
			this.shortName = shortName;
		}
		
		
		@Override
		public String getName() {
			return shortName;
		}
		
		
		@Override
		public String toString() {
			return super.getName();
		}
	}
	
}
