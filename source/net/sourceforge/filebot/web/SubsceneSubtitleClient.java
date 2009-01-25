
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getHtmlDocument;
import static net.sourceforge.tuned.XPathUtil.selectNode;
import static net.sourceforge.tuned.XPathUtil.selectNodes;
import static net.sourceforge.tuned.XPathUtil.selectString;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
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

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.FileUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class SubsceneSubtitleClient implements SubtitleClient {
	
	private static final String host = "subscene.com";
	
	private final Map<String, Integer> languageFilterMap = new ConcurrentHashMap<String, Integer>(50);
	
	
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
			String title = selectString("text()", node);
			String href = selectString("@href", node);
			String count = selectString("./DFN", node).replaceAll("\\D+", "");
			
			try {
				URL subtitleListUrl = new URL("http", host, href);
				int subtitleCount = Integer.parseInt(count);
				
				searchResults.add(new SubsceneSearchResult(title, subtitleListUrl, subtitleCount));
			} catch (MalformedURLException e) {
				Logger.getLogger("global").log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		// we might have been redirected to the subtitle list
		if (searchResults.isEmpty()) {
			int subtitleNodeCount = getSubtitleNodes(dom).size();
			
			// check if document is a subtitle list
			if (subtitleNodeCount > 0) {
				
				// get name of current search result
				String name = selectString("id('leftWrapperWide')//H1/text()", dom);
				
				// get current location
				String file = selectString("id('aspnetForm')/@action", dom);
				
				try {
					URL url = new URL("http", host, file);
					
					searchResults.add(new SubsceneSearchResult(name, url, subtitleNodeCount));
				} catch (MalformedURLException e) {
					Logger.getLogger("global").log(Level.WARNING, "Invalid location: " + file, e);
				}
			}
		}
		
		return searchResults;
	}
	

	private void updateLanguageFilterMap(Document subtitleListDocument) {
		
		List<Node> nodes = selectNodes("//DIV[@class='languageList']/DIV", subtitleListDocument);
		
		for (Node node : nodes) {
			String onClick = selectString("./INPUT/@onclick", node);
			
			String filter = new Scanner(onClick).findInLine("\\d+");
			
			if (filter != null) {
				String name = selectString("./LABEL/text()", node);
				
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
		
		URL subtitleListUrl = getSubtitleListLink(searchResult, language).toURL();
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
		return ((SubsceneSearchResult) searchResult).getSubtitleCount() > 50;
	}
	

	private Document getSubtitleListDocument(URL subtitleListUrl, Integer languageFilter) throws IOException, SAXException {
		URLConnection connection = subtitleListUrl.openConnection();
		
		if (languageFilter != null) {
			connection.addRequestProperty("Cookie", "subscene_sLanguageIds=" + languageFilter);
		}
		
		return getHtmlDocument(connection);
	}
	

	private List<Node> getSubtitleNodes(Document subtitleListDocument) {
		return selectNodes("//TABLE[@class='filmSubtitleList']//A[@id]//ancestor::TR", subtitleListDocument);
	}
	

	private List<SubtitleDescriptor> getSubtitleList(URL subtitleListUrl, String languageName, List<Node> subtitleNodes) {
		
		Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', .*");
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>(subtitleNodes.size());
		
		for (Node node : subtitleNodes) {
			try {
				Node linkNode = selectNode("./TD[1]/A", node);
				String lang = selectString("./SPAN[1]", linkNode);
				
				if (languageName == null || languageName.equalsIgnoreCase(lang)) {
					
					String href = selectString("@href", linkNode);
					String name = selectString("./SPAN[2]", linkNode);
					String author = selectString("./TD[4]", node);
					
					Matcher matcher = hrefPattern.matcher(href);
					
					if (!matcher.matches())
						throw new IllegalArgumentException("Cannot parse download parameters: " + href);
					
					String subtitleId = matcher.group(1);
					String typeId = matcher.group(2);
					
					URL downloadUrl = getDownloadUrl(subtitleListUrl, subtitleId, typeId);
					
					subtitles.add(new SubsceneSubtitleDescriptor(name, lang, author, typeId, downloadUrl, subtitleListUrl));
				}
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return subtitles;
	}
	

	private URL getDownloadUrl(URL referer, String subtitleId, String typeId) throws MalformedURLException {
		String basePath = FileUtilities.getNameWithoutExtension(referer.getFile());
		String path = String.format("%s-dlpath-%s/%s.zipx", basePath, subtitleId, typeId);
		
		return new URL(referer.getProtocol(), referer.getHost(), path);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, Locale locale) {
		return ((HyperLink) searchResult).toURI();
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
