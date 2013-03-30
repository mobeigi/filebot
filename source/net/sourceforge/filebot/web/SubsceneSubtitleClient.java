
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

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


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
		URL searchUrl = new URL("http", host, "/subtitles/title.aspx?q=" + encode(query, true));
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("//H2[text()='Close']//following::DIV[@class='title']//A", dom);
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
		
		return searchResults;
	}
	
	
	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		URL subtitleListUrl = getSubtitleListLink(searchResult, languageName).toURL();
		
		String filter = getLanguageFilter(languageName);
		Document dom = getSubtitleListDocument(subtitleListUrl, filter);
		
		List<Node> rows = selectNodes("//TD[@class='a1']", dom);
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		for (Node row : rows) {
			try {
				List<Node> fields = selectNodes(".//SPAN", row);
				String language = getTextContent(fields.get(0));
				
				if (languageName == null || language.equalsIgnoreCase(languageName)) {
					String name = getTextContent(fields.get(1));
					String href = selectString(".//A/@href", row);
					URL subtitlePage = new URL(subtitleListUrl.getProtocol(), subtitleListUrl.getHost(), href);
					subtitles.add(new SubsceneSubtitleDescriptor(name, language, subtitlePage));
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
			connection.addRequestProperty("Cookie", "Filter=" + languageFilter);
		}
		
		return getHtmlDocument(connection);
	}
	
	
	@SuppressWarnings("unchecked")
	protected String getLanguageFilter(String languageName) throws IOException, SAXException {
		if (languageName == null || languageName.isEmpty()) {
			return null;
		}
		
		// try cache first
		Cache cache = Cache.getCache("web-datasource-lv2");
		String cacheKey = getClass().getName() + ".languageFilter";
		
		Map<String, String> filters = cache.get(cacheKey, Map.class);
		
		if (filters != null) {
			return filters.get(languageName.toLowerCase());
		}
		
		// fetch new language filter data
		filters = getLanguageFilterMap();
		
		// update cache after sanity check
		if (filters.size() > 42) {
			cache.put(cacheKey, filters);
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to scrape language filters: " + filters);
		}
		
		return filters.get(languageName.toLowerCase());
	}
	
	
	protected Map<String, String> getLanguageFilterMap() throws IOException, SAXException {
		Map<String, String> filters = new HashMap<String, String>(50);
		
		Document dom = getHtmlDocument(new URL("http://subscene.com/filter"));
		List<Node> checkboxes = selectNodes("//INPUT[@type='checkbox']", dom);
		
		for (Node checkbox : checkboxes) {
			String filter = getAttribute("value", checkbox);
			if (filter != null) {
				String name = selectString("./following::LABEL", checkbox);
				filters.put(name.toLowerCase(), filter);
			}
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
