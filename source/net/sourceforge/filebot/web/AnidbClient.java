
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class AnidbClient extends EpisodeListClient {
	
	private NavigableMap<String, URL> cache = new TreeMap<String, URL>(String.CASE_INSENSITIVE_ORDER);
	
	private String host = "anidb.info";
	
	
	public AnidbClient() {
		super("AniDB", ResourceManager.getIcon("search.anidb"), false);
	};
	

	@Override
	public List<String> search(String searchterm) throws IOException, SAXException {
		synchronized (cache) {
			if (getFoundName(searchterm) != null) {
				return Arrays.asList(getFoundName(searchterm));
			}
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='animelist']//TR/TD/ancestor::TR", dom);
		
		LinkedHashMap<String, URL> searchResults = new LinkedHashMap<String, URL>(nodes.size());
		
		if (!nodes.isEmpty())
			for (Node node : nodes) {
				String type = XPathUtil.selectString("./TD[contains(@class,'type')]", node);
				
				// we only want shows
				if (type.equalsIgnoreCase("tv series")) {
					Node titleNode = XPathUtil.selectNode("./TD[@class='name']/A", node);
					
					String title = XPathUtil.selectString(".", titleNode);
					String href = XPathUtil.selectString("@href", titleNode);
					
					String file = "/perl-bin/" + href;
					
					try {
						URL url = new URL("http", host, file);
						
						searchResults.put(title, url);
					} catch (MalformedURLException e) {
						Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href);
					}
				}
			}
		else {
			// we might have been redirected to the episode list page directly
			List<Node> list = XPathUtil.selectNodes("//TABLE[@class='eplist']", dom);
			
			if (!list.isEmpty()) {
				// get show's name from the document
				String header = XPathUtil.selectString("id('layout-content')//H1[1]", dom);
				String title = header.replaceFirst("Anime:\\s*", "");
				
				searchResults.put(title, getSearchUrl(searchterm));
			}
		}
		
		synchronized (cache) {
			cache.putAll(searchResults);
		}
		
		return new ArrayList<String>(searchResults.keySet());
	}
	

	@Override
	public List<Episode> getEpisodeList(String showname, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListUrl(showname, season));
		
		List<Node> nodes = XPathUtil.selectNodes("id('eplist')//TR/TD/SPAN/ancestor::TR", dom);
		
		ArrayList<Episode> list = new ArrayList<Episode>(nodes.size());
		
		NumberFormat f = NumberFormat.getInstance();
		f.setMinimumIntegerDigits(Math.max(Integer.toString(nodes.size()).length(), 2));
		f.setGroupingUsed(false);
		
		for (Node node : nodes) {
			String number = XPathUtil.selectString("./TD[contains(@class,'id')]/A", node);
			String title = XPathUtil.selectString("./TD[@class='title']/LABEL/text()", node);
			
			if (title.startsWith("recap"))
				title = title.replaceFirst("recap", "");
			
			try {
				// try to format number of episode
				number = f.format(Integer.parseInt(number));
			} catch (NumberFormatException ex) {
				// leave it be
			}
			
			list.add(new Episode(showname, null, number, title));
		}
		
		return list;
	}
	

	@Override
	public URL getEpisodeListUrl(String showname, int season) {
		synchronized (cache) {
			return cache.get(showname);
		}
	}
	

	@Override
	public String getFoundName(String searchterm) {
		synchronized (cache) {
			if (cache.containsKey(searchterm)) {
				return cache.floorKey(searchterm);
			}
		}
		
		return null;
	}
	

	private URL getSearchUrl(String searchterm) throws IOException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/perl-bin/animedb.pl?show=animelist&orderby=name&orderdir=0&adb.search=" + qs + "&noalias=1&notinml=0";
		
		return new URL("http", host, file);
	}
	
}
