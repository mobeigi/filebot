
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class SubsceneSubtitleClient extends SubtitleClient {
	
	private final Map<MovieDescriptor, URL> cache = Collections.synchronizedMap(new HashMap<MovieDescriptor, URL>());
	
	private final String host = "subscene.com";
	
	
	public SubsceneSubtitleClient() {
		super("Subscene", ResourceManager.getIcon("search.subscene"));
	}
	

	@Override
	public List<MovieDescriptor> search(String searchterm) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('filmSearch')/A", dom);
		
		ArrayList<MovieDescriptor> results = new ArrayList<MovieDescriptor>(nodes.size());
		
		for (Node node : nodes) {
			String title = XPathUtil.selectString("text()", node);
			String href = XPathUtil.selectString("@href", node);
			
			try {
				URL url = new URL("http", host, href);
				
				MovieDescriptor descriptor = new MovieDescriptor(title);
				cache.put(descriptor, url);
				results.add(descriptor);
			} catch (MalformedURLException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		return results;
	}
	

	@Override
	public List<SubsceneSubtitleDescriptor> getSubtitleList(MovieDescriptor descriptor) throws IOException, SAXException {
		URL url = cache.get(descriptor);
		
		Document dom = HtmlUtil.getHtmlDocument(url);
		
		String downloadPath = XPathUtil.selectString("id('aspnetForm')/@action", dom);
		String viewstate = XPathUtil.selectString("id('__VIEWSTATE')/@value", dom);
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='filmSubtitleList']//A[@id]//ancestor::TR", dom);
		
		ArrayList<SubsceneSubtitleDescriptor> list = new ArrayList<SubsceneSubtitleDescriptor>();
		
		for (Node node : nodes) {
			try {
				Node linkNode = XPathUtil.selectFirstNode("./TD[1]/A", node);
				
				String href = XPathUtil.selectString("@href", linkNode);
				
				String lang = XPathUtil.selectString("./SPAN[1]", linkNode);
				String name = XPathUtil.selectString("./SPAN[2]", linkNode);
				
				int numberOfCDs = Integer.parseInt(XPathUtil.selectString("./TD[2]", node));
				boolean hearingImpaired = XPathUtil.selectFirstNode("./TD[3]/*[@id='imgEar']", node) != null;
				String author = XPathUtil.selectString("./TD[4]", node);
				
				URL downloadUrl = new URL("http", host, downloadPath);
				
				Map<String, String> downloadParameters = parseParameters(href);
				downloadParameters.put("__VIEWSTATE", viewstate);
				
				list.add(new SubsceneSubtitleDescriptor(name, lang, numberOfCDs, author, hearingImpaired, downloadUrl, downloadParameters));
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return list;
	}
	

	private Map<String, String> parseParameters(String href) {
		Matcher matcher = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', '0', '(\\d+)'\\);").matcher(href);
		
		if (!matcher.matches())
			throw new IllegalArgumentException("Cannot extract download parameters: " + href);
		
		Map<String, String> map = new HashMap<String, String>();
		
		map.put("subtitleId", matcher.group(1));
		map.put("typeId", matcher.group(2));
		map.put("filmId", matcher.group(3));
		
		return map;
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/filmsearch.aspx?q=" + qs;
		return new URL("http", host, file);
	}
	
}
