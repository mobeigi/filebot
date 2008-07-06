
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class AnidbClient extends EpisodeListClient {
	
	private final SearchResultCache cache = new SearchResultCache();
	
	private final String host = "anidb.info";
	
	
	public AnidbClient() {
		super("AniDB", ResourceManager.getIcon("search.anidb"));
	};
	

	@Override
	public List<SearchResult> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Collections.singletonList(cache.get(searchterm));
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='animelist']//TR/TD/ancestor::TR", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		if (!nodes.isEmpty()) {
			for (Node node : nodes) {
				Node titleNode = XPathUtil.selectNode("./TD[@class='name']/A", node);
				
				String title = XPathUtil.selectString(".", titleNode);
				String href = XPathUtil.selectString("@href", titleNode);
				
				String path = "/perl-bin/" + href;
				
				try {
					searchResults.add(new HyperLink(title, new URL("http", host, path)));
				} catch (MalformedURLException e) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href);
				}
			}
		} else {
			// we might have been redirected to the episode list page directly
			List<Node> list = XPathUtil.selectNodes("//TABLE[@class='eplist']", dom);
			
			if (!list.isEmpty()) {
				// get show's name from the document
				String header = XPathUtil.selectString("id('layout-content')//H1[1]", dom);
				String title = header.replaceFirst("Anime:\\s*", "");
				
				searchResults.add(new HyperLink(title, getSearchUrl(searchterm)));
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListLink(searchResult));
		
		List<Node> nodes = XPathUtil.selectNodes("id('eplist')//TR/TD/SPAN/ancestor::TR", dom);
		
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		numberFormat.setMinimumIntegerDigits(Math.max(Integer.toString(nodes.size()).length(), 2));
		numberFormat.setGroupingUsed(false);
		
		ArrayList<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String number = XPathUtil.selectString("./TD[contains(@class,'id')]/A", node);
			String title = XPathUtil.selectString("./TD[@class='title']/LABEL/text()", node);
			
			if (title.startsWith("recap"))
				title = title.replaceFirst("recap", "");
			
			try {
				// try to format number of episode
				number = numberFormat.format(Integer.parseInt(number));
			} catch (NumberFormatException ex) {
				// leave it be
			}
			
			// no seasons for anime
			episodes.add(new Episode(searchResult.getName(), null, number, title));
		}
		
		return episodes;
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return ((HyperLink) searchResult).toURI();
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return false;
	}
	

	@Override
	public Collection<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		throw new UnsupportedOperationException();
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		
		// type=2 -> only TV Series
		String path = "/perl-bin/animedb.pl?type=2&show=animelist&orderby.name=0.1&orderbar=0&noalias=1&do.search=Search&adb.search=" + qs;
		
		return new URL("http", host, path);
	}
	
}
