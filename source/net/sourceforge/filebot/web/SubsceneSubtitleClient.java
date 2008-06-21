
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.FunctionIterator;
import net.sourceforge.tuned.ProgressIterator;
import net.sourceforge.tuned.XPathUtil;
import net.sourceforge.tuned.FunctionIterator.Function;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class SubsceneSubtitleClient extends SubtitleClient {
	
	private final SearchResultCache cache = new SearchResultCache();
	
	private final String host = "subscene.com";
	
	
	public SubsceneSubtitleClient() {
		super("Subscene", ResourceManager.getIcon("search.subscene"));
	}
	

	@Override
	public List<SearchResult> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Collections.singletonList(cache.get(searchterm));
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('filmSearch')/A", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String title = XPathUtil.selectString("text()", node);
			String href = XPathUtil.selectString("@href", node);
			
			try {
				URL url = new URL("http", host, href);
				
				searchResults.add(new HyperLink(title, url));
			} catch (MalformedURLException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public ProgressIterator<SubtitleDescriptor> getSubtitleList(SearchResult searchResult) throws Exception {
		URL url = getSubtitleListLink(searchResult).toURL();
		
		Document dom = HtmlUtil.getHtmlDocument(url);
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='filmSubtitleList']//A[@id]//ancestor::TR", dom);
		
		return new FunctionIterator<Node, SubtitleDescriptor>(nodes, new SubtitleFunction(url));
	}
	
	
	private static class SubtitleFunction implements Function<Node, SubtitleDescriptor> {
		
		private final Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', '0', '(\\d+)'\\);");
		
		private final URL url;
		
		
		public SubtitleFunction(URL url) {
			this.url = url;
		}
		

		@Override
		public SubtitleDescriptor evaluate(Node node) throws Exception {
			Node linkNode = XPathUtil.selectFirstNode("./TD[1]/A", node);
			
			String href = XPathUtil.selectString("@href", linkNode);
			
			String lang = XPathUtil.selectString("./SPAN[1]", linkNode);
			String name = XPathUtil.selectString("./SPAN[2]", linkNode);
			
			String author = XPathUtil.selectString("./TD[4]", node);
			
			Matcher matcher = hrefPattern.matcher(href);
			
			if (!matcher.matches())
				throw new IllegalArgumentException("Cannot extract download parameters: " + href);
			
			String subtitleId = matcher.group(1);
			String typeId = matcher.group(2);
			
			URL downloadUrl = getDownloadUrl(url, subtitleId, typeId);
			
			return new SubsceneSubtitleDescriptor(name, lang, author, typeId, downloadUrl, url);
		}
		

		private URL getDownloadUrl(URL referer, String subtitleId, String typeId) throws MalformedURLException {
			String basePath = FileUtil.getNameWithoutExtension(referer.getFile());
			String path = String.format("%s-dlpath-%s/%s.zipx", basePath, subtitleId, typeId);
			
			return new URL(referer.getProtocol(), referer.getHost(), path);
		}
		
	}
	
	
	@Override
	public URI getSubtitleListLink(SearchResult searchResult) {
		return ((HyperLink) searchResult).getUri();
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/filmsearch.aspx?q=" + qs;
		return new URL("http", host, file);
	}
	
}
