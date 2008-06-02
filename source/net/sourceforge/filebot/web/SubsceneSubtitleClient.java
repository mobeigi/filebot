
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

import net.sourceforge.filebot.FileFormat;
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
		
		Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', '0', '(\\d+)'\\);");
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='filmSubtitleList']//A[@id]//ancestor::TR", dom);
		
		List<SubsceneSubtitleDescriptor> list = new ArrayList<SubsceneSubtitleDescriptor>();
		
		for (Node node : nodes) {
			try {
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
				
				list.add(new SubsceneSubtitleDescriptor(name, lang, author, typeId, downloadUrl, url));
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return list;
	}
	

	private URL getDownloadUrl(URL referer, String subtitleId, String typeId) throws MalformedURLException {
		String basePath = FileFormat.getNameWithoutExtension(referer.getFile());
		String path = String.format("%s-dlpath-%s/%s.zipx", basePath, subtitleId, typeId);
		
		return new URL(referer.getProtocol(), referer.getHost(), path);
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/filmsearch.aspx?q=" + qs;
		return new URL("http", host, file);
	}
	
}
