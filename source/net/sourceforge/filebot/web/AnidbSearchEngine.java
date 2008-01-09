
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.filebot.resources.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class AnidbSearchEngine extends SearchEngine {
	
	private Map<String, URL> cache = Collections.synchronizedMap(new TreeMap<String, URL>());
	
	private String host = "anidb.info";
	
	
	public AnidbSearchEngine() {
		super("AniDB", ResourceManager.getIcon("search.anidb"), false);
	};
	

	@Override
	public List<String> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Arrays.asList(searchterm);
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = HtmlUtil.selectNodes("//TABLE[@class='anime_list']//TR//TD//ancestor::TR", dom);
		ArrayList<String> shows = new ArrayList<String>(nodes.size());
		
		if (!nodes.isEmpty())
			for (Node node : nodes) {
				String type = HtmlUtil.selectString("./TD[2]/text()", node);
				
				// we only want shows
				if (type.equalsIgnoreCase("tv series")) {
					Node titleNode = HtmlUtil.selectNode("./TD[1]/A", node);
					
					String title = HtmlUtil.selectString("text()", titleNode);
					String href = HtmlUtil.selectString("@href", titleNode);
					
					String file = "/perl-bin/" + href;
					
					try {
						URL url = new URL("http", host, file);
						
						cache.put(title, url);
						shows.add(title);
					} catch (MalformedURLException e) {
						System.err.println("Invalid href: " + href);
					}
				}
			}
		else {
			// we might have been redirected to the episode list page directly
			List<Node> results = HtmlUtil.selectNodes("//TABLE[@class='eplist']", dom);
			
			if (!results.isEmpty()) {
				// get show's name from the document
				String header = HtmlUtil.selectString("//DIV[@id='layout-content']//H1[1]/text()", dom);
				String title = header.replaceFirst("Anime:\\s*", "");
				
				cache.put(title, getSearchUrl(searchterm));
				shows.add(title);
			}
			
		}
		
		return shows;
	}
	

	@Override
	public List<Episode> getEpisodeList(String showname, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListUrl(showname, season));
		
		List<Node> nodes = HtmlUtil.selectNodes("//TABLE[@id='eplist']//TR/TD/SPAN/ancestor::TR", dom);
		
		LinkedList<Episode> list = new LinkedList<Episode>();
		
		NumberFormat f = NumberFormat.getInstance();
		f.setMinimumIntegerDigits(Math.max(Integer.toString(nodes.size()).length(), 2));
		f.setGroupingUsed(false);
		
		for (Node node : nodes) {
			String number = HtmlUtil.selectString("./TD[1]/A/text()", node);
			String title = HtmlUtil.selectString("./TD[2]/SPAN/text()", node);
			
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
		return cache.get(showname);
	}
	

	private URL getSearchUrl(String searchterm) throws IOException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/perl-bin/animedb.pl?show=animelist&orderby=name&orderdir=0&adb.search=" + qs + "&noalias=1&notinml=0";
		
		return new URL("http", host, file);
	}
}
