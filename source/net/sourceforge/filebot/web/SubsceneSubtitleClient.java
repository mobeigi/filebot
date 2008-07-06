
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.XPathUtil;

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
				//TODO which exception?
				URI url = new URI("http", host, href);
				
				searchResults.add(new HyperLink(title, url));
			} catch (URISyntaxException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	
	HashMap<String, String> languageIdCache;
	
	
	public String getLanguageID(Locale language) {
		return languageIdCache.get(language.getDisplayLanguage(Locale.ENGLISH).toLowerCase());
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception {
		
		URL url = getSubtitleListLink(searchResult).toURL();
		
		Document dom = null;
		
		if (languageIdCache != null) {
			URLConnection connection = url.openConnection();
			
			if (language != null && language != Locale.ROOT) {
				System.out.println(getLanguageID(language));
				connection.addRequestProperty("Cookie", "subscene_sLanguageIds=" + getLanguageID(language));
			}
			
			dom = HtmlUtil.getHtmlDocument(connection);
		} else {
			URLConnection connection = url.openConnection();
			
			dom = HtmlUtil.getHtmlDocument(connection);
			
			List<Node> nodes = XPathUtil.selectNodes("//DIV[@class='languageList']/DIV", dom);
			
			Pattern onClickPattern = Pattern.compile("selectLanguage\\((\\d+)\\);");
			
			languageIdCache = new HashMap<String, String>();
			
			for (Node node : nodes) {
				Matcher matcher = onClickPattern.matcher(XPathUtil.selectString("./INPUT/@onclick", node));
				
				if (matcher.matches()) {
					String name = XPathUtil.selectString("./LABEL/text()", node);
					String id = matcher.group(1);
					
					//TODO sysout
					System.out.println(name + " = " + id);
					
					languageIdCache.put(name.toLowerCase(), id);
				}
			}
		}
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='filmSubtitleList']//A[@id]//ancestor::TR", dom);
		
		Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', '\\d+', '(\\d+)'\\);");
		
		ArrayList<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>(nodes.size());
		
		for (Node node : nodes) {
			try {
				Node linkNode = XPathUtil.selectFirstNode("./TD[1]/A", node);
				
				String lang = XPathUtil.selectString("./SPAN[1]", linkNode);
				
				String href = XPathUtil.selectString("@href", linkNode);
				
				String name = XPathUtil.selectString("./SPAN[2]", linkNode);
				
				String author = XPathUtil.selectString("./TD[4]", node);
				
				Matcher matcher = hrefPattern.matcher(href);
				
				if (!matcher.matches())
					throw new IllegalArgumentException("Cannot extract download parameters: " + href);
				
				String subtitleId = matcher.group(1);
				String typeId = matcher.group(2);
				
				URL downloadUrl = getDownloadUrl(url, subtitleId, typeId);
				
				subtitles.add(new SubsceneSubtitleDescriptor(name, lang, author, typeId, downloadUrl, url));
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
		return ((HyperLink) searchResult).getURI();
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/filmsearch.aspx?q=" + qs;
		return new URL("http", host, file);
	}
	
}
