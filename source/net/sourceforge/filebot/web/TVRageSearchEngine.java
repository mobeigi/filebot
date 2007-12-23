
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.resources.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVRageSearchEngine extends SearchEngine {
	
	private Map<String, URL> cache = Collections.synchronizedMap(new TreeMap<String, URL>(String.CASE_INSENSITIVE_ORDER));
	
	private String host = "www.tvrage.com";
	
	
	public TVRageSearchEngine() {
		super("TVRage", ResourceManager.getIcon("search.tvrage"), true);
	}
	

	@Override
	public List<String> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Arrays.asList(searchterm);
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = HtmlUtil.selectNodes("//DIV[@id='search_begin']//TABLE[1]//TR/TD/A[1]", dom);
		
		ArrayList<String> shows = new ArrayList<String>(nodes.size());
		
		for (Node node : nodes) {
			String href = HtmlUtil.selectString("@href", node);
			String title = HtmlUtil.selectString("text()", node);
			
			try {
				URL url = new URL(href);
				cache.put(title, url);
				shows.add(title);
			} catch (MalformedURLException e) {
				System.err.println("Invalid href: " + href);
			}
		}
		
		return shows;
	}
	

	@Override
	public List<Episode> getEpisodeList(String showname, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListUrl(showname, season));
		
		List<Node> nodes = HtmlUtil.selectNodes("//TABLE[@class='b']//TR[@id='brow']", dom);
		
		ArrayList<Episode> episodes = new ArrayList<Episode>();
		
		for (Node node : nodes) {
			String seasonAndEpisodeNumber = HtmlUtil.selectString("./TD[2]/A/text()", node);
			String title = HtmlUtil.selectString("./TD[4]/A/text()", node);
			
			List<Node> precedings = HtmlUtil.selectNodes("../preceding-sibling::TABLE", node);
			Node previousTable = precedings.get(precedings.size() - 1);
			
			String seasonHeader = HtmlUtil.selectString("./TR/TD/FONT/text()", previousTable);
			
			Matcher seasonMatcher = Pattern.compile("Season (\\d+)").matcher(seasonHeader);
			
			if (seasonMatcher.matches()) {
				if (season == 0 || season == Integer.parseInt(seasonMatcher.group(1))) {
					Matcher saeMatcher = Pattern.compile("(\\d+)x(\\d+)").matcher(seasonAndEpisodeNumber);
					
					String seasonNumber = null;
					String episodeNumber = null;
					
					if (saeMatcher.matches()) {
						seasonNumber = saeMatcher.group(1);
						episodeNumber = saeMatcher.group(2);
					} else {
						episodeNumber = seasonAndEpisodeNumber;
					}
					
					episodes.add(new Episode(showname, seasonNumber, episodeNumber, title));
				}
			}
		}
		
		return episodes;
	}
	

	@Override
	public URL getEpisodeListUrl(String showname, int season) {
		try {
			URL baseUrl = cache.get(showname);
			
			String seasonString = "all";
			
			if (season >= 1)
				seasonString = Integer.toString(season);
			
			String file = baseUrl.getFile() + "/episode_list/" + seasonString;
			return new URL("http", host, file);
		} catch (Exception e) {
			throw new RuntimeException("Cannot determine URL of episode listing for " + showname, e);
		}
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/search.php?search=" + qs;
		return new URL("http", host, file);
	}
}
