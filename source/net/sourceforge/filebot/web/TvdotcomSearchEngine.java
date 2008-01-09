
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.filebot.resources.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TvdotcomSearchEngine extends SearchEngine {
	
	private Map<String, URL> cache = Collections.synchronizedMap(new TreeMap<String, URL>());
	
	private String host = "www.tv.com";
	
	
	public TvdotcomSearchEngine() {
		super("TV.com", ResourceManager.getIcon("search.tvdotcom"), true);
	}
	

	@Override
	public List<String> search(String searchterm) throws UnsupportedEncodingException, MalformedURLException, IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Arrays.asList(searchterm);
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = HtmlUtil.selectNodes("//html:TABLE[@id='search-results']//html:SPAN/html:A", dom);
		
		ArrayList<String> shows = new ArrayList<String>(nodes.size());
		
		for (Node node : nodes) {
			String category = node.getParentNode().getTextContent();
			
			// we only want search results that are shows
			if (category.toLowerCase().startsWith("show")) {
				String title = node.getTextContent();
				String href = HtmlUtil.selectString("@href", node);
				
				try {
					URL url = new URL(href);
					
					cache.put(title, url);
					shows.add(title);
				} catch (MalformedURLException e) {
					System.err.println("Invalid href: " + href);
				}
			}
		}
		
		return shows;
	}
	

	@Override
	public List<Episode> getEpisodeList(String showname, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListUrl(showname, season));
		
		List<Node> nodes = HtmlUtil.selectNodes("//html:DIV[@id='episode-listing']/html:DIV/html:TABLE/html:TR/html:TD/ancestor::html:TR", dom);
		
		String seasonString = null;
		
		if (season >= 1)
			seasonString = Integer.toString(season);
		
		ArrayList<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMinimumIntegerDigits(Math.max(Integer.toString(nodes.size()).length(), 2));
		numberFormat.setGroupingUsed(false);
		
		Integer episodeOffset = null;
		
		if (season == 1)
			episodeOffset = 0;
		
		for (Node node : nodes) {
			String episodeNumber = HtmlUtil.selectString("./html:TD[1]/text()", node);
			String title = HtmlUtil.selectString("./html:TD[2]/html:A/text()", node);
			
			try {
				// format number of episode
				int n = Integer.parseInt(episodeNumber);
				
				if (episodeOffset == null)
					episodeOffset = n - 1;
				
				episodeNumber = numberFormat.format(n - episodeOffset);
			} catch (NumberFormatException e) {
				// episode number can be "Pilot" or "Special"
			}
			
			episodes.add(new Episode(showname, seasonString, episodeNumber, title));
		}
		
		return episodes;
	}
	

	public URL getEpisodeListUrl(String showname, int season) {
		try {
			String summaryFile = cache.get(showname).getFile();
			
			String base = summaryFile.substring(0, summaryFile.indexOf("summary.html"));
			String episodelistFile = base + "episode_listings.html&season=" + season;
			
			return new URL("http", host, episodelistFile);
		} catch (Exception e) {
			throw new RuntimeException("Cannot determine URL of episode listing for " + showname, e);
		}
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/search.php?qs=" + qs + "&type=11&stype=all";
		
		return new URL("http", host, file);
	}
}
