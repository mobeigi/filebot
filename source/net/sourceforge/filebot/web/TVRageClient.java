
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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


public class TVRageClient extends EpisodeListClient {
	
	private Map<String, URL> cache = Collections.synchronizedMap(new HashMap<String, URL>());
	
	private String host = "www.tvrage.com";
	
	
	public TVRageClient() {
		super("TVRage", ResourceManager.getIcon("search.tvrage"), true);
	}
	

	@Override
	public List<String> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Arrays.asList(searchterm);
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('search_begin')//TABLE[1]/*/TR/TD/A[1]", dom);
		
		ArrayList<String> shows = new ArrayList<String>(nodes.size());
		
		for (Node node : nodes) {
			String href = XPathUtil.selectString("@href", node);
			String title = XPathUtil.selectString(".", node);
			
			try {
				URL url = new URL(href);
				cache.put(title, url);
				shows.add(title);
			} catch (MalformedURLException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		return shows;
	}
	

	@Override
	public List<Episode> getEpisodeList(String showname, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListUrl(showname, season));
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='b']//TR[@id='brow']", dom);
		
		ArrayList<Episode> episodes = new ArrayList<Episode>();
		
		for (Node node : nodes) {
			String seasonAndEpisodeNumber = XPathUtil.selectString("./TD[2]/A", node);
			String title = XPathUtil.selectString("./TD[5]/A", node);
			
			List<Node> precedings = XPathUtil.selectNodes("../preceding-sibling::TABLE", node);
			Node previousTable = precedings.get(precedings.size() - 1);
			
			String seasonHeader = XPathUtil.selectString("./TR/TD/FONT", previousTable);
			
			Matcher seasonMatcher = Pattern.compile("Season (\\d+)").matcher(seasonHeader);
			
			if (seasonMatcher.matches()) {
				if ((season == 0) || (season == Integer.parseInt(seasonMatcher.group(1)))) {
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
