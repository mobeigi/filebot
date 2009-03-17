
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getHtmlDocument;
import static net.sourceforge.tuned.XPathUtilities.exists;
import static net.sourceforge.tuned.XPathUtilities.getAttribute;
import static net.sourceforge.tuned.XPathUtilities.getTextContent;
import static net.sourceforge.tuned.XPathUtilities.selectNode;
import static net.sourceforge.tuned.XPathUtilities.selectNodes;
import static net.sourceforge.tuned.XPathUtilities.selectString;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class AnidbClient implements EpisodeListClient {
	
	private static final String host = "anidb.net";
	
	
	@Override
	public String getName() {
		return "AniDB";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.anidb");
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		
		// type=2 -> only TV Series
		URL searchUrl = new URL("http", host, "/perl-bin/animedb.pl?type=2&show=animelist&orderby.name=0.1&orderbar=0&noalias=1&do.search=Search&adb.search=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("//TABLE[@class='animelist']//TR/TD/ancestor::TR", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			Node titleNode = selectNode("./TD[@class='name']/A", node);
			
			String title = getTextContent(titleNode);
			String href = getAttribute("href", titleNode);
			
			try {
				searchResults.add(new HyperLink(title, new URL("http", host, "/perl-bin/" + href)));
			} catch (MalformedURLException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid href: " + href);
			}
		}
		
		// we might have been redirected to the episode list page
		if (searchResults.isEmpty()) {
			// check if current page contains an episode list
			if (exists("//TABLE[@class='eplist']", dom)) {
				// get show's name from the document
				String header = selectString("id('layout-content')//H1[1]", dom);
				String name = header.replaceFirst("Anime:\\s*", "");
				
				String episodeListUrl = selectString("id('layout-main')//DIV[@class='data']//A[@class='short_link']/@href", dom);
				
				try {
					searchResults.add(new HyperLink(name, new URL(episodeListUrl)));
				} catch (MalformedURLException e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid location: " + episodeListUrl);
				}
			}
		}
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws IOException, SAXException {
		
		Document dom = getHtmlDocument(getEpisodeListLink(searchResult).toURL());
		
		List<Node> nodes = selectNodes("id('eplist')//TR/TD/SPAN/ancestor::TR", dom);
		
		ArrayList<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String number = selectString("./TD[contains(@class,'id')]/A", node);
			String title = selectString("./TD[@class='title']/LABEL/text()", node);
			
			if (title.startsWith("recap")) {
				title = title.replaceFirst("recap", "");
			}
			
			// if number does not match, episode is probably some kind of special (S1, S2, ...)
			if (number.matches("\\d+")) {
				// no seasons for anime
				episodes.add(new Episode(searchResult.getName(), number, title));
			}
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
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return null;
	}
	
}
