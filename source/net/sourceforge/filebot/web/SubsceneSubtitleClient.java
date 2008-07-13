
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class SubsceneSubtitleClient implements SubtitleClient {
	
	private final SearchResultCache searchResultCache = new SearchResultCache();
	
	private final Map<String, Integer> languageFilterMap = new ConcurrentHashMap<String, Integer>(50);
	
	private final String host = "subscene.com";
	
	
	@Override
	public String getName() {
		return "Subscene";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.subscene");
	}
	

	@Override
	public List<SearchResult> search(String searchterm) throws IOException, SAXException {
		if (searchResultCache.containsKey(searchterm)) {
			return Collections.singletonList(searchResultCache.get(searchterm));
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('filmSearch')/A", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String title = XPathUtil.selectString("text()", node);
			String href = XPathUtil.selectString("@href", node);
			String count = XPathUtil.selectString("./DFN", node).replaceAll("\\D+", "");
			
			try {
				URL subtitleListUrl = new URL("http", host, href);
				int subtitleCount = Integer.parseInt(count);
				
				searchResults.add(new SubsceneSearchResult(title, subtitleListUrl, subtitleCount));
			} catch (MalformedURLException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		// we might have been redirected to the subtitle list
		if (searchResults.isEmpty()) {
			int subtitleNodeCount = getSubtitleNodes(dom).size();
			
			// check if document is a subtitle list
			if (subtitleNodeCount > 0) {
				
				// get name of current search result
				String name = XPathUtil.selectString("id('leftWrapperWide')//H1/text()", dom);
				
				// get current url
				String file = XPathUtil.selectString("id('aspnetForm')/@action", dom);
				
				try {
					URL url = new URL("http", host, file);
					
					searchResults.add(new SubsceneSearchResult(name, url, subtitleNodeCount));
				} catch (MalformedURLException e) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid location: " + file, e);
				}
			}
		}
		
		searchResultCache.addAll(searchResults);
		
		return searchResults;
	}
	

	private void updateLanguageFilterMap(Document subtitleListDocument) {
		
		List<Node> nodes = XPathUtil.selectNodes("//DIV[@class='languageList']/DIV", subtitleListDocument);
		
		for (Node node : nodes) {
			String onClick = XPathUtil.selectString("./INPUT/@onclick", node);
			
			String filter = new Scanner(onClick).findInLine("\\d+");
			
			if (filter != null) {
				String name = XPathUtil.selectString("./LABEL/text()", node);
				
				languageFilterMap.put(name.toLowerCase(), Integer.valueOf(filter));
			}
		}
	}
	

	private Integer getLanguageFilter(String languageName) {
		if (languageName == null)
			return null;
		
		return languageFilterMap.get(languageName.toLowerCase());
	}
	

	private String getLanguageName(Locale language) {
		if (language == null || language == Locale.ROOT)
			return null;
		
		return language.getDisplayLanguage(Locale.ENGLISH);
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception {
		
		URL subtitleListUrl = getSubtitleListLink(searchResult).toURL();
		String languageName = getLanguageName(language);
		Integer languageFilter = getLanguageFilter(languageName);
		
		boolean reloadFilteredDocument = (languageFilter == null && useFilteredDocument(searchResult));
		boolean forceReload = false;
		
		if (reloadFilteredDocument && languageFilterMap.isEmpty()) {
			// we don't know the filter values yet, so we request a document with an invalid filter,
			// that will return a subtitle document very fast
			languageFilter = -1;
			forceReload = true;
		}
		
		Document subtitleListDocument = getSubtitleListDocument(subtitleListUrl, languageFilter);
		
		// let's update language filters if they are not known yet
		if (languageFilterMap.isEmpty()) {
			updateLanguageFilterMap(subtitleListDocument);
		}
		
		// check if document is already filtered and if requesting a filtered document 
		// will result in a performance gain (Note: XPath can be very slow)
		if (reloadFilteredDocument) {
			languageFilter = getLanguageFilter(languageName);
			
			// if language filter has become available, request a filtered document, or if first request was a dummy request
			if (languageFilter != null || forceReload) {
				subtitleListDocument = getSubtitleListDocument(subtitleListUrl, languageFilter);
			}
		}
		
		return getSubtitleList(subtitleListUrl, languageName, getSubtitleNodes(subtitleListDocument));
	}
	

	private boolean useFilteredDocument(SearchResult searchResult) {
		SubsceneSearchResult sr = (SubsceneSearchResult) searchResult;
		return sr.getSubtitleCount() > 50;
	}
	

	private Document getSubtitleListDocument(URL subtitleListUrl, Integer languageFilter) throws IOException, SAXException {
		Map<String, String> requestHeaders = new HashMap<String, String>(1);
		
		if (languageFilter != null) {
			requestHeaders.put("Cookie", "subscene_sLanguageIds=" + languageFilter);
		}
		
		return HtmlUtil.getHtmlDocument(subtitleListUrl, requestHeaders);
	}
	

	private List<Node> getSubtitleNodes(Document subtitleListDocument) {
		return XPathUtil.selectNodes("//TABLE[@class='filmSubtitleList']//A[@id]//ancestor::TR", subtitleListDocument);
	}
	

	private List<SubtitleDescriptor> getSubtitleList(URL subtitleListUrl, String languageName, List<Node> subtitleNodes) {
		
		Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', .*");
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>(subtitleNodes.size());
		
		for (Node node : subtitleNodes) {
			try {
				Node linkNode = XPathUtil.selectFirstNode("./TD[1]/A", node);
				String lang = XPathUtil.selectString("./SPAN[1]", linkNode);
				
				if (languageName == null || languageName.equalsIgnoreCase(lang)) {
					
					String href = XPathUtil.selectString("@href", linkNode);
					String name = XPathUtil.selectString("./SPAN[2]", linkNode);
					String author = XPathUtil.selectString("./TD[4]", node);
					
					Matcher matcher = hrefPattern.matcher(href);
					
					if (!matcher.matches())
						throw new IllegalArgumentException("Cannot extract download parameters: " + href);
					
					String subtitleId = matcher.group(1);
					String typeId = matcher.group(2);
					
					URL downloadUrl = getDownloadUrl(subtitleListUrl, subtitleId, typeId);
					
					subtitles.add(new SubsceneSubtitleDescriptor(name, lang, author, typeId, downloadUrl, subtitleListUrl));
				}
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return subtitles;
	}
	

	private URL getDownloadUrl(URL referer, String subtitleId, String typeId) throws MalformedURLException {
		String basePath = FileUtil.getNameWithoutExtension(referer.getFile());
		String path = String.format("%s-dlpath-%s/%s.zipx", basePath, subtitleId, typeId);
		
		return new URL(referer.getProtocol(), referer.getHost(), path);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult) {
		return ((HyperLink) searchResult).toURI();
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/filmsearch.aspx?q=" + qs;
		return new URL("http", host, file);
	}
	
	
	protected static class SubsceneSearchResult extends HyperLink {
		
		private final int subtitleCount;
		
		
		public SubsceneSearchResult(String name, URL url, int subtitleCount) {
			super(name, url);
			this.subtitleCount = subtitleCount;
		}
		

		public int getSubtitleCount() {
			return subtitleCount;
		}
		
	}
	
}
